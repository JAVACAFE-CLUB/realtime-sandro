#!/usr/bin/env python3
"""환경변수 설정 테스트 스크립트"""

import sys
from pathlib import Path

# 프로젝트 루트를 Python 경로에 추가
project_root = Path(__file__).parent.parent
src_path = project_root / "src"
sys.path.insert(0, str(src_path))

from harvest.services.upload_service import UploadService
import os
from dotenv import load_dotenv

def test_env_variables():
    """환경변수 설정 테스트"""
    print("=== 환경변수 설정 테스트 ===")
    
    # .env 파일 로드
    load_dotenv()
    
    # 환경변수 값 확인
    print(f"WIKI_DATA_DIR: {os.getenv('WIKI_DATA_DIR')}")
    print(f"WIKI_BUCKET_NAME: {os.getenv('WIKI_BUCKET_NAME')}")
    
    # UploadService 인스턴스 생성 (환경변수 사용)
    try:
        uploader = UploadService()
        print(f"✅ UploadService 생성 성공")
        print(f"   - data_dir: {uploader.data_dir}")
        print(f"   - bucket_name: {uploader.bucket_name}")
    except Exception as e:
        print(f"❌ UploadService 생성 실패: {e}")
    
    # 명시적 파라미터로 테스트
    try:
        uploader2 = UploadService(data_dir="/tmp", bucket_name="test-bucket")
        print(f"✅ 명시적 파라미터 UploadService 생성 성공")
        print(f"   - data_dir: {uploader2.data_dir}")
        print(f"   - bucket_name: {uploader2.bucket_name}")
    except Exception as e:
        print(f"❌ 명시적 파라미터 UploadService 생성 실패: {e}")

if __name__ == "__main__":
    test_env_variables()