#!/usr/bin/env python3
"""위키 데이터 업로드 실행 스크립트"""

import sys
from pathlib import Path

# 프로젝트 루트를 Python 경로에 추가
project_root = Path(__file__).parent
src_path = project_root / "src"
sys.path.insert(0, str(src_path))

from harvest.upload_wiki_data import main

if __name__ == "__main__":
    main()