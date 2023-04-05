package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity {

    private String createdBy;

    private LocalDateTime createDate;

    private String lastModifiedBy;

    private LocalDateTime lastModifiedDate;
}
