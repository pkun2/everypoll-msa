#!/bin/bash
# 로컬 개발 환경 전체 초기화: MySQL 볼륨과 rag-agent의 chroma_db를 함께 삭제
set -euo pipefail

docker compose -f docker-compose.yml down -v
docker compose -f rag-agent/docker-compose-ai.yml down -v
rm -rf rag-agent/chroma_db

echo "초기화 완료: MySQL 볼륨과 chroma_db가 모두 삭제되었습니다."
