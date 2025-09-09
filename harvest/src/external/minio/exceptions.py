"""MinIO 관련 커스텀 예외 클래스"""


class MinIOError(Exception):
    """MinIO 관련 기본 예외 클래스"""
    pass


class MinIOConnectionError(MinIOError):
    """MinIO 서버 연결 관련 예외"""
    pass


class MinIOBucketError(MinIOError):
    """MinIO 버킷 관련 예외"""
    pass


class MinIOOperationError(MinIOError):
    """MinIO 작업 수행 관련 예외"""
    pass


class MinIOConfigError(MinIOError):
    """MinIO 설정 관련 예외"""
    pass


class MinIOUploadError(MinIOOperationError):
    """MinIO 업로드 관련 예외"""
    pass


class MinIODownloadError(MinIOOperationError):
    """MinIO 다운로드 관련 예외"""
    pass
