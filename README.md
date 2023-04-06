# :bookmark:자바 ORM 표준 JPA 프로그래밍

# 영속성 컨텍스트
**"엔티티를 영구 저장하는 환경"**

## 이점
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

---
# 양방향 연관관계와 연관관계의 주인
## 연관관계의 주인
### 양방향 매핑 규칙
* 객체의 두 관계중 하나를 연관관계의 주인으로 지정
* **연관관계의 주인만이 외래 키를 관리(등록, 수정)**
* **주인이 아닌쪽은 읽기만 가능**
* 주인은 mappedBy 속성 사용X
* 주인이 아니면 mappedBy 속성으로 주인 지정

### 누구를 연관관계의 주인으로?
* 외래키가 있는 곳
* 다대일 관계에서 '다' 쪽이 외래키를 가지고 있기에 주인이다.(ex: Member(다)와 Team(일))
* 주인이 아닌 쪽은 mappedBy 적용(Team Entity에는 mappedBy 적용)

### 양방향 매핑 정리
* **단방향 매핑만으로도 이미 연관관계 매핑은 완료**
* 양방향 매핑은 반대 방향으로 조회(객체 그래프 탐색) 기능이 추가된 것 뿐
* JPQL에서 역방향으로 탐색할 일이 많음
* 단방향 매핑을 잘 하고 양방향은 필요할 때 추가해도 됨(테이블에 영향을 주지 않음)

---
# 즉시 로딩과 지연 로딩
## 지연 로딩(Lazy Loading)
* 객체 조회 시 연관되어 있는 엔티티들을 모두 조회하는 것 보다는 필요한 연관관계만 조회해 오는 것이 효과적.
* 이런 상황을 위해, JPA는 지연 로딩 방식을 지원하는데, FetchType = LAZY로 설정하면 연관된 것을 프록시로 조회한다.
> **프록시란?**
> * '대신하다'라는 의미를 가지고 있는 단어이며, 동작을 대신해주는 가짜 객체의 개념.
> * Hibernate가 지연 로딩을 구현하기 위해 프록시를 사용

> **프록시가 실제 객체처럼 동작 할 수 있는 이유?**
> * 프록시가 실제 객체를 상속한 타입을 가지고 있기 때문
> * 프록시 객체는 실제 객체에 대한 참조를 보관 => 프록시 객체의 메서드 호출 시, 실제 객체의 메서드를 호출

## 즉시 로딩(Eager Loading)
* 예를 들어, Member와 Team이 서로 있으면 지연 로딩과 다르게 Member를 로딩을 할 때, Team까지 같이 Join을 해서 가져온다.
* **가급적 지연 로딩만 사용(특히 실무에서)**
* 즉시 로딩을 적용하면 예상하지 못한 SQL이 발생
* 즉시 로딩은 JPQL에서 N+1 문제를 일으킨다
* @ManyToOne, @OneToOne은 기본이 즉시 로딩 -> LAZY로 설정
* @OneToMany, @ManyToMany는 기본이 지연 로딩

# 영속성 전이(CASCADE)
* 특정 엔티티를 영속 상태로 만들 때 연관된 엔티티도 함께 영속 상태로 만들고 싶을 때
* 부모 엔티티를 저장할 때 자식 엔티티도 함께 저장
* **(주의사항!) 만약 자식 엔티티가 다른 엔티티와도 연관되어 있으면 사용X**
```Java
// childList에 대해서도 영속성 전이
@OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
private List<Child> childList = new ArrayList<>();
```

# 임베디드 타입(Embedded Type)
* 주로 기본 값 타입을 모아서 만들어서 복합 값 타입이라고도 한다.

## 임베디드 타입 사용법
* @Embeddable: 값 타입을 정의하는 곳에 표시
* @Embedded: 값 타입을 사용하는 곳에 표시
* 기본 생성자 필수

```Java
   @Entity
   public class Member {
       //기간 Period
       @Embedded
       private Period workPeriod;
   }
```
```Java
   @Embeddable
   public class Period {
      private LocalDateTime startDate;
      private LocalDateTime endDate;
}
```
## 임베디드 타입과 테이블 매핑
* 임베디드 타입은 엔티티의 값일 뿐이다.
* 임베디드 타입을 사용하기 전과 후에 **매핑하는 테이블은 같다.**
* 객체와 테이블을 아주 세밀하게(find-grained) 매핑하는 것이 가능
* 잘 설계한 ORM 애플리케이션은 매핑한 테이블의 수보다 클래스의 수가 더 많음

# JPQL
* JPA를 사용하면 엔티티 객체를 중심으로 개발
* 문제는 검색 쿼리
* 검색을 할 때도 테이블이 아닌 엔티티 객체를 대상으로 검색
* 모든 DB 데이터를 객체로 변환해서 검색하는 것은 불가능
* 애플리케이션이 필요한 데이터만 DB에서 불러오려면 결국 검색 조건이 포함된 SQL이 필요
---
* JPA는 SQL을 추상화한 JPQL이라는 객체 지향 쿼리 언어 제공
* SQL과 문법 유사, SELECT, FROM, WHERE, GROUP BY, HAVING, JOIN 지원
* JPQL은 엔티티 객체를 대상으로 쿼리
* SQL은 데이터베이스 테이블을 대상으로 쿼리
* SQL을 추상화해서 특정 데이터베이스 SQL에 의존X
* JPQL을 한마디로 정의하면 객체 지향 SQL
```Java
//검색
String jpql = "select m From Member m where m.name like '%hello%'";
List<Member> result = em.createQuery(jpql, Member.class).getResultList();
```
하지만 위의 방식은 정적쿼리만 가능하다.
따라서 대안으로 **Criteria**를 사용하면 동적쿼리도 가능하게 하는데, **Criteria**예시를 살펴보자.
```Java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Member> query = cb.createQuery(Member.class);

//루트 클래스 (조회를 시작할 클래스)
Root<Member> m = query.from(Member.class);

//쿼리 생성 
CriteriaQuery<Member> cq = query.select(m).where(cb.equal(m.get("username", "kim"));

List<Member> resultList = em.createQuery(cq).getResultList();
```
하지만 Criteria는 너무 복잡하고 실용성이 없기에, Criteria 대신에 **QueryDSL**사용을 권장한다.
# QueryDSL
* 문자가 아닌 자바코드로 JPQL을 작성할 수 있음
* JPQL 빌더 역할
* 컴파일 시점에 문법 오류를 찾을 수 있음
* **동적쿼리** 작성 편리함
* 단순하고 쉬움
* **실무 사용 권장**
```Java
JPAFactoryQuery query = new JPAQueryFactory(em);
QMember m = QMmeber.member;

List<Member> list = query.selectFrom(m)
                          .where(m.age.gt(18))
                          .orderBy(m.name.desc())
                          .fetch();
```
QueryDSL은 이번강의에선 이정도만 알아보고, JPQL에 대해서 더 알아보기로 한다.
