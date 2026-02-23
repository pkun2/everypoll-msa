from aiokafka import AIOKafkaConsumer
import json
import asyncio
from typing import List, Dict, Callable, Any, Coroutine

class KafkaConsumerService:
    def __init__(self, bootstrap_servers: str, topics: List[str], handler_map: Dict[str, Callable[[Dict[str, Any]], Coroutine[Any, Any, None]]]) -> None:
        self.bootstrap_servers = bootstrap_servers
        self.topics = topics
        self.handler_map = handler_map
        self.consumer = None
        self._running = False

    async def start(self) -> None:
        self.consumer = AIOKafkaConsumer(
            *self.topics,
            bootstrap_servers=self.bootstrap_servers,
            group_id="rag-agent-group",
            value_deserializer=lambda x: json.loads(x.decode('utf-8'))
        )
        await self.consumer.start()
        self._running = True
        try:
            while self._running:
                try:
                    msg = await asyncio.wait_for(self.consumer.getone(), timeout=1.0)
                    
                    handler = self.handler_map.get(msg.topic)
                    if handler:
                        await handler(msg.value)
                        
                except asyncio.TimeoutError:
                    continue
                except Exception as e:
                    print(f"Error processing message: {e}")
        finally:
            print("Consumer Loop Logic Finished")
            if self.consumer:
                await self.consumer.stop()
    async def stop(self):
        self._running = False
