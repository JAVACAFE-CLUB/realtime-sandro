"""FastAPI 메인 애플리케이션"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .routers import health, upload, x

# FastAPI 앱 인스턴스 생성
app = FastAPI(
    title="Harvest Data Collector",
    version="0.1.0",
    description="데이터 수집 서버 - 위키, 뉴스, X API 데이터를 수집하여 MinIO에 저장",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS 설정 (개발 환경용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 특정 도메인으로 제한
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(health.router, prefix="/health", tags=["Health"])
app.include_router(upload.router, prefix="/upload", tags=["Upload"])
app.include_router(x.router, prefix="/x", tags=["X API"])


@app.get("/", tags=["Root"])
async def root():
    """API 루트 엔드포인트"""
    return {
        "name": "Harvest Data Collector",
        "version": "0.1.0",
        "docs": "/docs",
        "redoc": "/redoc",
        "endpoints": {
            "health": "/health",
            "upload_files": "/upload/files",
            "upload_all": "/upload/all",
            "upload_all_background": "/upload/all/background",
            "upload_single": "/upload/file/{filename}",
            "x_search": "/x/search",
            "x_timeline": "/x/timeline"
        }
    }
