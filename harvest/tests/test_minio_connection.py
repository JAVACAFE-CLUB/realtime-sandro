#!/usr/bin/env python3
"""MinIO 연결 테스트 스크립트"""

import sys
from pathlib import Path
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

# 프로젝트 루트를 Python 경로에 추가
project_root = Path(__file__).parent.parent
src_path = project_root / "src"
sys.path.insert(0, str(src_path))

from harvest.services.minio_service import MinIOService


def test_minio_connection():
    """MinIO 연결 테스트"""
    print("MinIO 연결 테스트를 시작합니다...")
    
    try:
        # MinIO 업로더 초기화
        uploader = MinIOService()
        print("✅ MinIO 클라이언트 초기화 성공")
        
        # 테스트 버킷 생성
        test_bucket = "test-bucket"
        if uploader.ensure_bucket_exists(test_bucket):
            print(f"✅ 버킷 '{test_bucket}' 생성/확인 성공")
        else:
            print(f"❌ 버킷 '{test_bucket}' 생성 실패")
            return False
        
        # 버킷 목록 조회 (간접적으로 연결 확인)
        try:
            # 빈 객체 목록이라도 정상적으로 반환되면 연결 성공
            objects = uploader.list_objects(test_bucket)
            print(f"✅ 버킷 '{test_bucket}' 객체 목록 조회 성공 (객체 수: {len(objects)}개)")
        except Exception as e:
            print(f"❌ 객체 목록 조회 실패: {e}")
            return False
        
        print("\n🎉 MinIO 연결 테스트 완료! 모든 기능이 정상 작동합니다.")
        print("\n다음 단계:")
        print("1. docker-compose up -d 명령으로 MinIO 서비스 시작")
        print("2. http://localhost:9001 에서 MinIO 웹 콘솔 접속")
        print("3. python upload_wiki_data.py 명령으로 위키 데이터 업로드")
        
        return True
        
    except Exception as e:
        print(f"❌ MinIO 연결 실패: {e}")
        print("\n문제 해결 방법:")
        print("1. docker-compose up -d 명령으로 MinIO 서비스가 시작되었는지 확인")
        print("2. docker ps 명령으로 realtime-minio 컨테이너가 실행 중인지 확인")
        print("3. http://localhost:9000 에서 MinIO API 접근 가능한지 확인")
        return False


if __name__ == "__main__":
    success = test_minio_connection()
    sys.exit(0 if success else 1)