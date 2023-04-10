# :bookmark:실전! 스프링 부트와 JPA 활용1

# 인텔리제이 유용한 단축키
* 테스트 클래스 생성: ctrl+shift+T(windows), command+shift+T(Linux)
* 해당 결과값 변수 뽑아오는 것: ctrl+alt+v
* 테스트 메서드 설정(settings -> template검색 후 -> Live Templates)에 tdd로 하나생성
```Java
@Test
public void $NAME$() throws Exception {
   //given
   $END$
   //when

   //then
}
```

JPA Test 케이스 예시
```Java
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @Transactional
    @Rollback(false)
    public void testMember() throws Exception {
        //given
        Member member = new Member();
        member.setUsername("memberA");

        //when
        Long saveId = memberRepository.save(member);
        Member findMember = memberRepository.find(saveId);

        //then
        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
        Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        Assertions.assertThat(findMember).isEqualTo(member);
    }
}
```

## 쿼리 파라미터 로그 남기기
* application.yml파일에 logging:level:org.hibernate.type: trace를 추가
* build.gradle에 **implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.5.6'** 추가
* 쿼리 파라미터를 로그로 남기는 외부 라이브러리는 시스템 자원을 사용하므로, 개발 단계에서는 편하게 사용해도 됨. 
* 하지만 운영시스템에 적용하려면 꼭 성능테스트를 하고 사용하는 것이 좋다.

# 엔티티에서 상속관계 매핑시 어노테이션
* 부모 클래스에서 @Inheritance(strategy = InheritanceType.SINGLE_TABLE) => 단일 테이블 전략
* 부모 클래스에서 @DiscriminatorColumn(name = "dtype")
* 자식 클래스에서 @DiscriminatorValue("임의의 문자")

# 엔티티에서 테이블 관계가 OneToOne일 때
* 한 쪽 엔티티를 관계의 주인으로 잡고(fk가 있는곳) 일대다, 다대일과 마찬가지로 JoinColumn과 mappedBy로 연결시켜주면 된다.

# 실무에서는 @ManyToMany는 되도록 사용하지 말자!

# (중요!) 실무에서 엔티티에서 Setter를 사용하지 않는 이유?
* Setter를 호출하면 데이터가 변하는데, Setter를 막 열어두면 가까운 미래에 엔티티가 도대체 왜 변경되는지 추적하기 점점 힘들어진다.
* 그래서 엔티티를 변경할 때는 Setter대신에 변경 지점이 명확하도록 변경을 위한 비즈니스 메서드를 제공해야한다.(추후 @Builder)

# 값 타입(예를 들어 Address) 엔티티
* 값 타입은 변경 불가능하게 설계해야 한다.
* JPA 스펙상 엔티티나 임베디드 타입(@Embeddable)은 자바 기본 생성자(default constructor)를 **public** 또는 **protected**로 설정해야한다.
* JPA가 이런 제약을 두는 이유는 JPA 구현 라이브러리가 객체를 생성할 때 리플렉션 같은 기술을 사용할 수 있도록 지원해야 하기 때문.

**예시**
```Java
import lombok.Getter;

import javax.persistence.Embeddable;

@Embeddable
@Getter
public class Address {

    private String city;
    private String street;
    private String zipcode;
}
```
```Java
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @Embedded
    private Address address;
}
```
