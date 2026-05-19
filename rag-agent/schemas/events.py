from typing import Optional
from pydantic import BaseModel, Field

class PollCreatedEvent(BaseModel):
    id: str = Field(..., description="투표 ID")
    title: str = Field(..., description="투표 제목")
    description: Optional[str] = Field(None, description="투표 설명")

class CommentCreatedEvent(BaseModel):
    poll_id: str = Field(..., alias="pollId", description="투표 ID")
    content: str = Field(..., description="댓글 내용")
