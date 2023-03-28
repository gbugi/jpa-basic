# :bookmark:자바 ORM 표준 JPA 프로그래밍

## 영속성 컨텍스트
**"엔티티를 영구 저장하는 환경"**

### 이점
* 1차 캐시
* 동일성(identity) 보장
* 트랜잭션을 지원하는 쓰기 지연(transactional write-behind)
* 변경 감지(Dirty Checking)
* 지연 로딩(Lazy Loading)
---
## 1차 캐시
네트워크를 통해 데이터베이스에 접근하는 시간 비용은 애플리케이션 서버 내부 메모리에 접근하는 시간보다 훨씬 비쌈.
따라서 조회한 데이터를 메모리에 캐싱해 두면 데이터베이스 접근 횟수를 줄여 성능을 개선할 수 있음.

JPA에서 1차 캐시는 **EntityManager가 관리하는 영속성 컨텍스트 내부에 있는 첫 번째 캐시**이다.
트랜잭션을 시작하고 종료할 때까지만 1차 캐시가 유효하다.

### 1차 캐시의 조회 동작
1. 조회 시 1차 캐시에 데이터가 이미 있는지 확인하고, 데이터가 있으면 가져온다. (비교는 PK로 한다.)
2. 1차 캐시에 데이터가 없다면, 데이터베이스에 데이터를 요청한다.
3. 데이터베이스에서 받은 데이터를 다음에 재사용할 수 있도록 1차 캐시에 저장한다.

> Q] Update가 발생하면 데이터가 갱신될텐데, 캐시에 있는 것을 조회하면 과거의 데이터만 받는거 아닌가요?   
> A] 1차 캐시는 영속성 컨텍스트 내부에 있습니다. 영속성 컨텍스트 내부에 있는 엔터티의 변화가 즉시 1차 캐시에 저장되기 때문에, 캐시에서 갱신된 데이터를 받게 됩니다.

### 1차 캐시의 쓰기 동작
1. 데이터가 변경되면 즉시 1차캐시에 반영.
2. 변경 사항이 지연 SQL 저장소에 저장됨.
3. Transaction이 commit되면 Flush가 발생.
4. 지연 SQL 저장소에 있는 SQL문을 DB에 요청.
<br/><br/>

## 동일성 보장
1차 캐시로 반복 가능한 읽기(REPEATABLE READ) 등급의 트랜잭션 격리 수준을 데이터베이스가 아닌 애플리케이션 차원에서 제공   
```Java
Member a = em.find(Member.class, "member1");
Member b = em.find(Member.class, "member1");

System.out.println(a == b); //동일성 비교 true
```
<br/>

## 쓰기 지연
Transaction commit이 일어날 때 **Flush(영속성 컨텍스트의 변경내용을 데이터베이스에 반영하는 것)** 가 동작하는데, **Flush**가 발생하면 쓰기 지연 SQL 저장소에 있는 쌓인 쿼리들(INSERT, UPDATE, DELETE Query)을 즉시 DB에 날린다. 즉, 그동안의 변경사항을 Flush가 일어날 때 한번에 기록함. 따라서, DB 접근 빈도를 최소화할 수 있음. 이를 **쓰기 지연**이라고 한다.

### 플러시의 동작 과정
1. 변경을 감지한다.(Dirty Checking)
2. 수정된 Entity를 쓰기 지연 SQL 저장소에 등록한다.
3. 쓰기 지연 SQL 저장소의 Query를 DB에 전송한다. (INSERT, UPDATE, DELETE Query)

```Java
// 트랙잭션을 지원하는 쓰기 지연
EntityManager em = emf.createEntityManager(); //emf는 EntityManagerFactory
EntityTransaction transaction = em.getTransaction();
//엔티티 매니저는 데이터 변경시 트랜잭션을 시작해야 한다.
transaction.begin(); // [트랜잭션] 시작

em.persist(memberA);
em.persist(memberB);
//여기까지 INSERT SQL을 데이터베이스에 보내지 않는다.

//커밋하는 순간 데이터베이스에 INSERT SQL을 보냄.
transaction.commit(); // [트랜잭션] 커밋
```
   
### 영속성 컨텍스트를 플러시하는 방법(자동 호출을 권함)
1. em.flush() - 직접 호출
2. 트랜잭션 커밋 - 플러시 자동 호출
3. JPQL 쿼리 실행 - 플러시 자동 호출
<br/>

### 플러시와 영속성 컨텍스트
1. 플러시는 영속성 컨텍스트를 비우지 않음
2. 영속성 컨텍스트의 변경내용을 데이터베이스에 동기화 하는것임
3. **트랜잭션**이라는 작업 단위가 중요 -> 커밋 직전에만 동기화 하면 됨
<br/>

## 변경 감지(Dirty Checking)
<img src="https://user-images.githubusercontent.com/75151693/227857264-61d0c438-e927-4ebe-bcf5-556bec74af30.png" width = "400" height = "300">   
영속성 컨텍스트가 관리하는 엔티티가 수정되면 지연 SQL 저장소에 쿼리문이 저장된다.

```Java
// 영속 엔티티 조회
Member memberA = em.find(Member.class, "memberA");

//영속 엔티티 데이터 수정
memberA.setName("zz");

transaction.commit();
```

> em.persist() 호출해야 되는거아니야? No!   
> 이유?
> - JPA의 목적: 자바 컬렉션 다루듯 객체를 다루기 위함
> - 예시) 자바 List같은 컬렉션에서 값을 꺼내고 변경한 뒤 다시 컬렉션에 넣어준다? No -> JPA도 같은원리임!
