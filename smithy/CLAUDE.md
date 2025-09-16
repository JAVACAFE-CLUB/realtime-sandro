# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Smithy는 realtime 프로젝트의 데이터 정제(Data Processing) 모듈입니다. Harvest 모듈이 수집한 원시 데이터를 처리하여 정제된 형태로 변환합니다.

## Technology Stack

- **Language**: Python 3.12+
- **Package Manager**: uv
- **Dependencies**: boto3, confluent-kafka, newspaper3k

## Build & Development Commands

- **Install dependencies**: `uv sync`
- **Run the application**: `uv run python -m smithy`
- **Install new package**: `uv add <package-name>`
- **Remove package**: `uv remove <package-name>`

## Architecture

### Data Processing Pipeline

Smithy는 Kafka Consumer로 동작하며, 다음과 같은 데이터 처리 파이프라인을 가집니다:

1. **Kafka Message Processing**: Harvest에서 전송된 카프카 메시지 수신
2. **News Processing**: 
   - MinIO에서 HTML 조회
   - newspaper3K를 이용한 핵심 콘텐츠 추출 (제목, 본문)
   - 해시 기반 중복 제거 (Redis 저장)
3. **Wikipedia Processing**:
   - 문서 파싱 및 ID 확인
   - RevisionID 기반 중복 검사 및 버전 관리
   - 원본 URL 생성 (siteinfo.base + page.title)

### Key Components

- **Data Deduplication**: 해시 함수와 Redis를 활용한 중복 제거 시스템
- **Content Extraction**: newspaper3K를 이용한 뉴스 콘텐츠 추출
- **Version Management**: Wikipedia RevisionID 기반 버전 관리
- **Storage Integration**: MinIO 객체 스토리지 연동

## Directory Structure

```
smithy/
├── pyproject.toml          # Python project configuration
├── uv.lock                 # Dependency lock file  
├── smithy.md              # Module specification
└── bin/                   # Legacy Kotlin structure (unused)
```

## Important Notes

- 이 모듈은 Kotlin에서 Python으로 전환되었습니다
- bin/ 디렉토리의 Kotlin 구조는 레거시입니다
- uv 패키지 매니저를 사용합니다
- Kafka, Redis, MinIO와의 연동이 필요합니다