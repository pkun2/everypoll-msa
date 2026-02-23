import asyncio
from typing import List, Dict, Callable, Coroutine, Any
from datetime import datetime, timedelta
from apscheduler.schedulers.asyncio import AsyncIOScheduler

class CommentBatcher:
    def __init__(self, callback: Callable[[str, List[str]], Coroutine[Any, Any, None]], max_size: int = 50, interval_minutes: int = 10) -> None:
        self.buffer: Dict[str, List[str]] = {} # poll_id -> [comments]
        self.max_size = max_size
        self.callback = callback
        self.scheduler = AsyncIOScheduler()
        self.scheduler.add_job(self._check_interval, 'interval', minutes=1)
        self.last_flush: Dict[str, datetime] = {}

    def start(self):
        self.scheduler.start()

    async def add_comment(self, poll_id: str, comment: str):
        if poll_id not in self.buffer:
            self.buffer[poll_id] = []
            self.last_flush[poll_id] = datetime.now()
        
        self.buffer[poll_id].append(comment)
        
        if len(self.buffer[poll_id]) >= self.max_size:
            await self._flush(poll_id)

    async def _check_interval(self) -> None:
        now = datetime.now()
        ids_to_flush = [
            pid for pid, last in self.last_flush.items() 
            if now - last >= timedelta(minutes=10) and self.buffer.get(pid)
        ]
        for pid in ids_to_flush:
            await self._flush(pid)

    async def _flush(self, poll_id: str) -> None:
        comments = self.buffer.pop(poll_id, [])
        self.last_flush[poll_id] = datetime.now()
        if comments:
            print(f"📦 Flashing {len(comments)} comments for poll {poll_id}")
            # LLM 요약 호출 (비동기)
            asyncio.create_task(self.callback(poll_id, comments))

    def stop(self) -> None:
        self.scheduler.shutdown()
