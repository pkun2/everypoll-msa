from aiokafka import AIOKafkaConsumer
import json
import asyncio
from typing import List, Dict, Callable, Any, Coroutine

class KafkaConsumerService:
    def __init__(self, bootstrap_servers: str, topics: List[str], handler_map: Dict[str, Callable[[Dict[str, Any]], Coroutine[Any, Any, None]]]) -> None:
        self.bootstrap_servers = bootstrap_servers
        self.topics = topics
        self.handler_map = handler_map # topic_name -> async handler function
        self.consumer: Any = None

    async def start(self) -> None:
        self.consumer = AIOKafkaConsumer(
            *self.topics,
            bootstrap_servers=self.bootstrap_servers,
            group_id="rag-agent-group",
            value_deserializer=lambda x: json.loads(x.decode('utf-8'))
        )
        await self.consumer.start()
        try:
            async for msg in self.consumer:
                handler = self.handler_map.get(msg.topic)
                if handler:
                    await handler(msg.value)
        finally:
            await self.consumer.stop()

    async def stop(self) -> None:
        if self.consumer:
            await self.consumer.stop()
