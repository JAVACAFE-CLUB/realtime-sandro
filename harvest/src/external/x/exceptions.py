"""X API 예외 클래스 정의"""


class XAPIError(Exception):
    """X API 기본 예외 클래스"""
    pass


class XAPIConnectionError(XAPIError):
    """X API 연결 오류"""
    pass


class XAPIAuthenticationError(XAPIError):
    """X API 인증 오류"""
    pass


class XAPIRateLimitError(XAPIError):
    """X API Rate Limit 오류"""
    pass


class XAPIConfigError(XAPIError):
    """X API 설정 오류"""
    pass


class XAPISearchError(XAPIError):
    """X API 검색 오류"""
    pass


class XAPITimelineError(XAPIError):
    """X API 타임라인 오류"""
    pass
