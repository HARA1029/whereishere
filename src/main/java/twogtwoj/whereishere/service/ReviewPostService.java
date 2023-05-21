package twogtwoj.whereishere.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import twogtwoj.whereishere.domain.*;
import twogtwoj.whereishere.dto.*;
import twogtwoj.whereishere.file.*;
import twogtwoj.whereishere.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewPostService {

    private final ReviewPostRepository reviewPostRepository;

    private final MemberRepository memberRepository;

    private final ReviewLikeRepository reviewLikeRepository;

    private final FileStore fileStore;


    //글 작성 처리
    public void post(ReviewPostDto reviewPostDto,Member member) throws Exception {

        ReviewPost reviewPost = new ReviewPost();

        reviewPost.setMember(member);
        reviewPost.setWriter(member.getName());
        reviewPost.setReviewPostDate(LocalDateTime.now());
        reviewPost.setName(reviewPostDto.getName());
        reviewPost.setReviewPostContent(reviewPostDto.getReviewPostContent());
        reviewPost.setReviewPostTitle(reviewPostDto.getReviewPostTitle());

        MultipartFile file1 = reviewPostDto.getFile1();
        MultipartFile file2 = reviewPostDto.getFile2();

        UploadFile uploadFile1 = fileStore.storeFile(file1);
        UploadFile uploadFile2 = fileStore.storeFile(file2);

        reviewPost.setReviewPostImg1(uploadFile1.getStoreFileName());
        reviewPost.setReviewPostImg2(uploadFile2.getStoreFileName());

        reviewPostRepository.save(reviewPost);
    }

    public void update(Long reviewPostId, ReviewPostDto reviewPostDto) throws Exception {
        ReviewPost reviewPost = reviewPostRepository.findById(reviewPostId).get();

        reviewPost.setReviewPostTitle(reviewPostDto.getReviewPostTitle());
        reviewPost.setReviewPostContent(reviewPostDto.getReviewPostContent());//덮어씌우기
        reviewPost.setName(reviewPostDto.getName());
        reviewPost.setReviewPostDate(LocalDateTime.now());

        // 이미지 파일이 null이 아니라면 수정

        // null이 아니면
        // 기존 파일을 새로운 파일로 변경
        // 1. 기존 파일을 제거
        // 2. 새로운 파일을 등록
        if (reviewPostDto.getFile1().getOriginalFilename().length() != 0) {
            fileStore.updateFile(reviewPost.getReviewPostImg1(), reviewPostDto.getFile1());
        }
        if (reviewPostDto.getFile2().getOriginalFilename().length() != 0) {
            fileStore.updateFile(reviewPost.getReviewPostImg2(), reviewPostDto.getFile2());
        }
        reviewPostRepository.save(reviewPost);
    }

    //게시글 리스트 처리
    public Page<ReviewPost> reviewPostList(Pageable pageable) {
        return reviewPostRepository.findAll(pageable);
    }

    //검색 리스트
    public Page<ReviewPost> reviewPostSearchList(String searchKeyword, String name, Pageable pageable) {
        if (searchKeyword == null) {
            searchKeyword = "";
        }
        if (name == null) {
            name = "";
        }
        return reviewPostRepository.findByReviewPostTitleContainingAndNameContaining(searchKeyword, name, pageable);
    }

    //특정 게시글 불러오기
    public ReviewPost reviewPostView(Long reviewPostId) {
        return reviewPostRepository.findById(reviewPostId).orElse(null);
    }

/*    // 상세페이지에서 이전글을 보여주는 메서드
    public ReviewPost[] showPreviousPosts(Long reviewPostId) {
       return reviewPostRepository.findPreviousPost(reviewPostId);
    }

    // 상세페이지 다음글을 보여주는 메서드
    public ReviewPost[] showNextPosts(Long reviewPostId) {
        return reviewPostRepository.findNextPost(reviewPostId);
    }*/

/*    public Optional<ReviewPost> showPreviousPost(Long reviewPostId) {
        return reviewPostRepository.findFirstByReviewPostIdLessThanOrderByReviewPostIdDesc(reviewPostId);
    }

    public Optional<ReviewPost> showNextPost(Long reviewPostId) {
        return reviewPostRepository.findFirstByReviewPostIdGreaterThanOrderByReviewPostId(reviewPostId);
    }*/

/*    public ReviewPost showPreviousPost(LocalDate reviewPostDate) {
        return reviewPostRepository.findPreviousPostByReviewPostDate(reviewPostDate);
    }

    public ReviewPost showNextPost(LocalDate reviewPostDate) {
        return reviewPostRepository.findNextPostByReviewPostDate(reviewPostDate);
    }*/

    //이전글 다음글
    public Page<ReviewPost> getReviewPosts(Pageable pageable, String searchKeyword, String name) {
    if (StringUtils.hasText(searchKeyword)) {
        return reviewPostRepository.findByReviewPostTitleContainingOrReviewPostContentContaining(pageable, searchKeyword, searchKeyword);
    } else if (StringUtils.hasText(name)) {
        return reviewPostRepository.findByMember_Name(pageable, name);
    } else {
        return reviewPostRepository.findAll(pageable);
    }
}

//이전글 다음글
    public List<ReviewPost> getAdjacentPostsByReviewPostDate(LocalDateTime reviewPostDate) {
        List<ReviewPost> adjacentPosts = new ArrayList<>();

        Optional<ReviewPost> previousPostOptional = reviewPostRepository.findTop1ByReviewPostDateBeforeOrderByReviewPostDateDesc(reviewPostDate);
        previousPostOptional.ifPresent(adjacentPosts::add);
        /*
        Optional 객체에 값이 있는지 확인 하는 메소드 위에는 아래와 같은 코드의 람다식
          if (previousPostOptional.isPresent()) {
            adjacentPosts.add(previousPostOptional.get());
        }
         */

        Optional<ReviewPost> nextPostOptional = reviewPostRepository.findTop1ByReviewPostDateAfterOrderByReviewPostDateAsc(reviewPostDate);
        nextPostOptional.ifPresent(adjacentPosts::add);
        /*
          Optional 객체에 값이 있는지 확인 하는 메소드 위에는 아래와 같은 코드의 람다식. 위에게 간결하고 가독성 좋은 코드
          optional 객체에서 값을 꺼내 리스트에 추가하는 기능을 수행
          if (nextPostOptional.isPresent()) {
            adjacentPosts.add(nextPostOptional.get());
        }
         */

        return adjacentPosts;
    }



    //특정 게시글 삭제
    public void delete(Long reviewPostId) {
        reviewPostRepository.deleteById(reviewPostId);
    }

    public int findLike(Long reviewPostId, Long memberId) {
        Optional<ReviewPost> reviewPost = reviewPostRepository.findById(reviewPostId);
        Optional<Member> member = memberRepository.findMemberByMemberId(memberId);

        if (reviewPost.isPresent() && member.isPresent()) {
            ReviewPost foundReviewPost = reviewPost.get();
            Member foundMember = member.get();

            Optional<ReviewLike> findLike = reviewLikeRepository.findByReviewPostAndMember(foundReviewPost, foundMember);

            if (findLike.isEmpty()) {
                return 0; // 좋아요 안 함
            } else {
                return 1; // 좋아요 함
            }
        }
        return -1; // 처리할 수 없는 경우
    }

    @Transactional
    public int saveLike(Long reviewPostId, Long memberId) {
        ReviewPost foundReviewPost = reviewPostRepository.findById(reviewPostId).get();
        Member foundMember = memberRepository.findMemberByMemberId(memberId).get();

        Optional<ReviewLike> findLike = reviewLikeRepository.findByReviewPostAndMember(foundReviewPost, foundMember);

        Member member = memberRepository.findMemberByMemberId(memberId).get();
        ReviewPost reviewPost = reviewPostRepository.findById(reviewPostId).get();

        if (findLike.isEmpty()) {
            ReviewLike reviewLike = ReviewLike.toReviewLike(member, reviewPost);
            reviewLikeRepository.save(reviewLike);
            reviewPostRepository.plusReviewLike(reviewPostId);
            //저장 : 1
            return 1;
        } else {
            reviewLikeRepository.deleteByReviewPostAndMember(reviewPost, member);
            reviewPostRepository.minusReviewLike(reviewPostId);
            //삭제 : 0
            return 0;
        }
    }

    @Transactional
    public void increaseViewCount(Long reviewPostId) {
        ReviewPost reviewPost = reviewPostRepository.findById(reviewPostId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + reviewPostId));
        reviewPost.increaseViewCount();
        reviewPostRepository.save(reviewPost);
    }

}