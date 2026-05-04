import asyncio
from typing import List, Dict, Callable, Coroutine, Any
from datetime import datetime, timedelta
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from core.metrics import BATCH_FLUSH_TOTAL, BATCH_FLUSH_SIZE, BATCH_BUFFER_SIZE

class CommentBatcher:
    def __init__(
        self,
        callback: Callable[[str, List[str]],
        Coroutine[Any, Any, None]],
        max_size: int = 5,
        interval_minutes: int = 10
    ) -> None:
        self.buffer: Dict[str, List[str]] = {}
        self.last_flush: Dict[str, datetime] = {}
        self.max_size = max_size
        self.interval_minutes = interval_minutes
        self.callback = callback
        self.scheduler = AsyncIOScheduler()
        self.lock = asyncio.Lock()
        self.semaphore = asyncio.Semaphore(3)

    def start(self):
        self.scheduler.add_job(self._check_interval, 'interval', minutes=1)
        self.scheduler.start()

    async def add_comment(self, poll_id: str, comment: str):
        async with self.lock:
            if poll_id not in self.buffer:
                self.buffer[poll_id] = []
                self.last_flush[poll_id] = datetime.now()

            self.buffer[poll_id].append(comment)
            BATCH_BUFFER_SIZE.set(sum(len(v) for v in self.buffer.values()))

            if len(self.buffer[poll_id]) >= self.max_size:
                await self._flush(poll_id, trigger="size")

    async def _check_interval(self) -> None:
        async with self.lock:
            now = datetime.now()
            ids_to_flush = [
                pid for pid, last in self.last_flush.items()
                if now - last >= timedelta(minutes=self.interval_minutes) and self.buffer.get(pid)
            ] # 10분 넘으면 flush
            for pid in ids_to_flush:
                await self._flush(pid, trigger="time")

    async def _flush(self, poll_id: str, trigger: str = "time") -> None:
        comments = self.buffer.pop(poll_id, [])
        # 오래된 기록 삭제 (메모리 관리)
        if poll_id in self.last_flush:
            del self.last_flush[poll_id]

        if comments:
            BATCH_FLUSH_TOTAL.labels(trigger=trigger).inc()
            BATCH_FLUSH_SIZE.observe(len(comments))
            BATCH_BUFFER_SIZE.set(sum(len(v) for v in self.buffer.values()))
            print(f"📦 [Flush] Poll: {poll_id}, Count: {len(comments)}, Trigger: {trigger}")
            asyncio.create_task(self._safe_callback(poll_id, comments))

    async def _safe_callback(self, poll_id: str, comments: List[str]):
        async with self.semaphore:
            try:
                await self.callback(poll_id, comments)
            except Exception as e:
                print(f"❌ 요약 실패 (Poll {poll_id}): {e}")

    def stop(self) -> None:
        self.scheduler.shutdown()
