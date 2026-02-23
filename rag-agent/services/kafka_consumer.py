from aiokafka import AIOKafkaConsumer
import json
import asyncio
import logging

logger = logging.getLogger(__name__)

class KafkaConsumerService:
    def __init__(self, bootstrap_servers: str, topics: list, handler_map: dict):
        self.bootstrap_servers = bootstrap_servers
        self.topics = topics
        self.handler_map = handler_map
        self.consumer = None
        self._running = False

    async def start(self):
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
                    logger.error(f"Error processing message: {e}")
        finally:
            logger.info("Consumer Loop Logic Finished")
            if self.consumer:
                await self.consumer.stop()
    async def stop(self):
        self._running = False
