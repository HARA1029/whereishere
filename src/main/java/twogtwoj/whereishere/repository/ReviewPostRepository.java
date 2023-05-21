package twogtwoj.whereishere.repository;

import lombok.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import twogtwoj.whereishere.domain.ReviewPost;

import java.beans.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReviewPostRepository extends JpaRepository<ReviewPost, Long>, JpaSpecificationExecutor<ReviewPost> {

    Page<ReviewPost> findByReviewPostTitleContainingAndNameContaining(String reviewPostTitle, String name, Pageable pageable);

  /*  // 이전글 가져오기. 글번호 순으로 정렬
    Optional<ReviewPost> findFirstByReviewPostIdLessThanOrderByReviewPostIdDesc(Long reviewPostId);
    // 다음글 가져오기
    Optional<ReviewPost> findFirstByReviewPostIdGreaterThanOrderByReviewPostId(Long reviewPostId);*/



    @Transactional
    @Modifying
    @Query(value = "update ReviewPost r set r.likeCount = r.likeCount+1 where r.reviewPostId = :reviewPostId")
    int plusReviewLike(@Param("reviewPostId") Long reviewPostId);

    @Transactional
    @Modifying
    @Query(value = "update ReviewPost r set r.likeCount = r.likeCount-1 where r.reviewPostId = :reviewPostId")
    int minusReviewLike(@Param("reviewPostId") Long reviewPostId);

    /*
    LAG 함수 : 기준 데이터의 이전 행의 값을 반환해주는 함수
    LEAD 함수 : 기준 데이터의 다음 행의 값을 반환해주는 함수
    SELECT 조회할 컬럼명 LAG(대상 컬럼명) OVER (ORDER BY 대상 컬럼명 정렬기준) AS 별칭 FROM 테이블명

    SELECT 조회할 컬럼명 LEAD(대상 컬럼명) OVER (ORDER BY 대상 컬럼명 정렬기준) AS 별칭 FROM 테이블명
     */

 /*   @Query(value = "SELECT r FROM ReviewPost r WHERE r.reviewPostId < :reviewPostId ORDER BY r.reviewPostId DESC")
    ReviewPost[] findPreviousPost(@Param("reviewPostId") Long reviewPostId);

    // 다음글 가져오기
    @Query(value = "SELECT r FROM ReviewPost r WHERE r.reviewPostId > :reviewPostId ORDER BY r.reviewPostId ASC")
    ReviewPost[] findNextPost(@Param("reviewPostId") Long reviewPostId);
*/

    Page<ReviewPost> findByReviewPostTitleContainingOrReviewPostContentContaining(Pageable pageable, String reviewPostTitle, String reviewPostContent);
    Page<ReviewPost> findByMember_Name(Pageable pageable, String memberName);

/*    @Query("SELECT r FROM ReviewPost r WHERE r.reviewPostDate < :reviewPostDate ORDER BY r.reviewPostDate DESC")
    Optional<ReviewPost> findPreviousPostByReviewPostDate(@Param("reviewPostDate") LocalDate reviewPostDate);

    @Query("SELECT r FROM ReviewPost r WHERE r.reviewPostDate > :reviewPostDate ORDER BY r.reviewPostDate ASC")
    Optional<ReviewPost> findNextPostByReviewPostDate(@Param("reviewPostDate") LocalDate reviewPostDate);*/


    Optional<ReviewPost> findTop1ByReviewPostDateBeforeOrderByReviewPostDateDesc(LocalDateTime reviewPostDate);
    Optional<ReviewPost> findTop1ByReviewPostDateAfterOrderByReviewPostDateAsc(LocalDateTime reviewPostDate);




}
