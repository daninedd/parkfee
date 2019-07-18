package com.daninedd.parkfee.parkfee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface FeeDetailDataRepository extends JpaRepository<FeeDetailData, Long> , QuerydslPredicateExecutor<FeeDetailData> {
}
