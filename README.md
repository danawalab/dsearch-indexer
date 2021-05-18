# DSearch 색인기 (라이브러리)

신규 검색엔진인 DSearch(이하 DS)는 Elastic Search (이하 ES)에 데이터를 저장하므로, 색인모듈이 필요하지 않다. 

하지만 데이터소스에서 ES로 데이터를 연결해주는 파이프라인의 역할은 여전히 필요하다.

파일 IO와 네트워크 IO에 집약된 작업이며 동시에 여러 인덱스가 실행할 수 있는 멀티 쓰레드방식으로 구현한다.

이 색인기는 항상 떠있다가 REST API로 색인요청을 받으면 색인 쓰레드를 만들어서 작업을 시작한다.


## Library JAR 생성 및 DSearch-server 적용 절차

1. 메이븐 패키징을 실행합니다.

2. target 디렉토리에 dsearch-indexer-<버전>.jar.origin 생성 확인합니다.

3. dsearch-indexer-<버전>.jar.origin 파일을 dsearch-server 프로젝트에서 lib에 추가합니다.

4. dsearch-indexer-<버전>.jar 과거 버전은 삭제 합니다.

5. dsearch-indexer-<버전>.jar.origin 이름을 dsearch-indexer-<버전>.jar 으로 변경합니다.

6. dsearch-server를 실행하여 색인 테스트를 진행합니다.

## 색인작업절차

1. 색인 쓰레드가 만들어진다.

2. POST /start 로 색인을 시작한다. 이때 색인할 정보를 전달한다. 향후 path는 달라질수 있다.

3. 시작중일때는 GET /status 로 상태를 확인한다. 현재 어떤 index를 몇건 색인하고 있는지 json으로 전달받는다.

4. 상태가 finished가 되면 색인이 종료된 것이다.



## 사용법

###1. 파일색인 요청

`POST http://localhost:5005/start`

```json
{
    "scheme": "http",
    "host": "es1.danawa.io",
    "port": 80,
    "index": "song5",
    "type": "ndjson",
    "path": "C:\\Projects\\fastcatx-indexer\\src\\test\\resources\\sample.ndjson",
    "encoding": "utf-8",
    "bulkSize": 1000
}
```

필수파라미터
- `scheme: string`: http, https
- `host: string` : ES 호스트주소
- `port: int` : ES 포트
- `index: string` : 인덱스명
- `bulkSize: int` : ES bulk API 사이즈
- `type: string` : 파서종류. ndjson, jdbc, csv..
- (옵션) `reset: boolean` : 디폴트 true. 색인전에 index가 존재하는지 확인하여 조재하면 delete 하고 색인진행
- (옵션) `filterClass: string` : 소스를 변환할 필터. 패키지명 포함. 생성자는 기본 생성자를 호출하게 됨. 예)com.danawa.fastcatx.indexer.filter.MockFilter 
- (옵션) `threadSize: int` : 색인 쓰레드 갯수. 수치가 높을수록 색인이 빨라지고 CPU사용률이 높다.
- (옵션) `pipeLine: string` : Ingest PipeLine. 사용할 파이프라인 네임을 입력

ndjson, cvs 파라미터
- `path: string` : 파일경로
- `encoding: string` : 파일인코딩
- (옵션) `limitSize: int` : 색인문서 제한갯수 

file 파라미터

- `headerText: string` : 파일텍스트 헤더네임 
- `delimiter: string` : 파일 구분 문자

```
ex) 아이폰^레드^10000

- headerText:"name,color,price"
- delimiter: "^"
```

JDBC 파라미터
- `driverClassName: string` : 드라이버이름(패키지 포함). 예) com.mysql.jdbc.Driver  
- `url: string` : JDBC URL
- `user: string` : 유저 아이디
- `password: string` : 유저 패스워드
- `dataSQL: string` : 색인 SQL문 
- `fetchSize: int` : JDBC fetch 사이즈. 1000정도가 무난. 문제발생시 -1사용.
- (옵션) `maxRows: int` : 디폴트 0. 색인문서 제한갯수.
- (옵션) `useBlobFile: boolean` : 디폴트 false. Blob 컬럼 사용여부. 
- `` :  

### 2. 상태확인

`GET http://localhost:8080/status`

색인시작전
```json
{
    "payload": {},
    "status": "READY"
}
```

진행중
```json
{
    "payload": {
        "scheme": "http",
        "host": "es1.danawa.io",
        "port": 80,
        "index": "song6",
        "type": "csv",
        "path": "C:\\Projects\\fastcatx-indexer\\sample\\food.csv",
        "encoding": "utf-8",
        "bulkSize": 1000
    },
    "startTime": 1591245998,
    "docSize": 231,
    "error": "",
    "status": "RUNNING"
}
```

완료
```json
{
    "payload": {
        "scheme": "http",
        "host": "es1.danawa.io",
        "port": 80,
        "index": "song6",
        "type": "csv",
        "path": "C:\\Projects\\fastcatx-indexer\\sample\\food.csv",
        "encoding": "utf-8",
        "bulkSize": 1000
    },
    "startTime": 1591245998,
    "docSize": 8130,
    "endTime": 1591246002,
    "error": "",
    "status": "SUCCESS"
}
```

### Konan -> json 컨버터
konan 수집형식을 ndjson 으로 변환해주는 유틸이다.

`java -cp indexer.jar com.danawa.fastcatx.indexer.KonanToJsonConverter <konan_file_path_or_directory> <file_encoding> <output_file_path>`

### jar파일로 색인기 시작하기.
`java -jar` 로는 classpath를 설정할 수 없다. 그래서 driver나 filter를 사용하려고 할때 외부 jar를 사용하기 어렵다. 그러므로 `-jar`옵션을 사용하지 말고 직접 spring boot 메인클래스를 실행해야 한다.

```
$ java -classpath indexer.jar:Altibase.jar:danawa-search.jar org.springframework.boot.loader.JarLauncher
```

### 동적색인 호출기 사용하기

`POST http://localhost:5005/dynamic`

```json
{
    "scheme": "http",
    "host": "es1.danawa.io",
    "port": 80,
    "index": "song5",
    "type": "ES",
    "path": "C:\\Projects\\fastcatx-indexer\\src\\test\\resources\\sample.ndjson",
    "encoding": "utf-8",
}
```

필수파라미터
- `scheme: string`: http, https
- `host: string` : 검색엔진 호스트 주소
- `port: int` : 검색엔진 포트
- `index: string` : 인덱스명( , 로 다수 입력)
- `type: string` : 검색엔진 종류 (FASTCAT, ES)


### 비동기 호출 사용방법
파일, 동적 요청과 동일한 파라미터를 사용하면됩니다. 차이점은 uri의 async 가 포함됩니다.   

`POST http://localhost:5005/async/start`

```json
{
    "scheme": "http",
    "host": "es1.danawa.io",
    "port": 80,
    "index": "song5",
    "type": "ndjson",
    "path": "C:\\Projects\\fastcatx-indexer\\src\\test\\resources\\sample.ndjson",
    "encoding": "utf-8",
    "bulkSize": 1000
}
```

### 파일 스트리밍 색인
DB 프로시저로 생성된 덤프파일을 RSYNC하고 RSYNC로 받아오는 파일을 즉시 읽어 색인

- `type : procedure` 로 지정하여 호출 

파라미터
- `scheme: string`: http, https
- `host: string` : 검색엔진 호스트 주소
- `port: int` : 검색엔진 포트
- `index: string` : 인덱스명
- `bulkSize: int` : ES bulk API 사이즈
- `driverClassName: string` : 드라이버이름(패키지 포함). 예) com.mysql.jdbc.Driver  
- `url: string` : JDBC URL
- `user: string` : 유저 아이디
- `password: string` : 유저 패스워드
- `procedureName: string` : 프로시저명 (default : PRSEARCHPRODUCT)
- `dumpFormat : string` : 덤프파일 타입 (konan , ndjson)
- `groupSeq: int` : groupSeq 번호
- `encoding: string` : 파일인코딩 
- `rsnycIp: string` : 전송받을 파일이 있는 서버 IP
- `path: string` : 전송 받을 파일경로
- (옵션) `bwlimit : string` : rsync 대여폭 (defualt : 0, 1024 = 1mb/s)
- (옵션) `procedureSkip : boolean` : 프로시저 스킵 (default : false)
- (옵션) `rsyncSkip : boolean` : rsync 스킵 (default : false)
- (옵션) `filterClass: string` : 소스를 변환할 필터. 패키지명 포함. 생성자는 기본 생성자를 호출하게 됨. 예)com.danawa.fastcatx.indexer.filter.MockFilter 
- (옵션) `threadSize: int` : 색인 쓰레드 갯수. 수치가 높을수록 색인이 빨라지고 CPU사용률이 높다.
- (옵션) `pipeLine: string` : Ingest PipeLine. 사용할 파이프라인 네임을 입력


```json
{
    "type": "procedure",
    "scheme": "http",
    "host": "es1.danawa.com",
    "port": 9200,
    "index": "search-prod-v0",
    "bulkSize": 1000,
    "filterClass":"com.danawa.fastcatx.indexer.filter.DanawaProductFilter",
    "driverClassName": "Altibase.jdbc.driver.AltibaseDriver",
    "url": "jdbc:Altibase://192.168.0.87",
    "user": "user1",
    "password": "user-password",
    "procedureName" :"procedure-a",
    "dumpFormat":"konan",
    "groupSeq":0,
    "bwlimit":"10240",
    "path":"/home/danawa/apps/indexer/file/V0",
    "rsyncIp":"192.168.0.87",
    "encoding":"CP949",
    "procedureSkip":true,
    "rsyncSkip":false,
    "threadSize":4
}
```