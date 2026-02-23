import re

class TextCleaner:
    def __init__(self):
        # 단순 반복 및 무의미한 패턴 (ㅋ, ㅎ, ㅠ 등)
        self.repeated_pattern = re.compile(r'([ㄱ-ㅎㅏ-ㅣ])\1{2,}')
        # 비속어 필터 (간단한 예시, 실제 운영시 마스킹 리스트 고도화 필요)
        self.slang_keywords = ["시발", "병신"] # 실운영시 외부 파일이나 DB에서 관리
        
    def is_meaningless(self, text: str) -> bool:
        """무의미한 짧은 댓글이나 단순 반복 필터링"""
        if len(text) > 5000: # DoS 방지: 너무 긴 텍스트는 처리하지 않음
            return True

        text = text.strip()
        if len(text) <= 2:
            return True
        if self.repeated_pattern.search(text):
            return True
        # "1등", "첫댓글" 등 단순 패턴
        if text in ["1등", "첫댓글", "ㅋㅋ"]:
            return True
        return False

    def clean(self, text: str) -> str:
        """기본적인 텍스트 정제"""
        # 불필요한 공백 제거
        text = " ".join(text.split())
        return text

    def has_slang(self, text: str) -> bool:
        """금칙어 포함 여부 확인 (동기 검사용)"""
        for slang in self.slang_keywords:
            if slang in text:
                return True
        return False
