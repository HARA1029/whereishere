package twogtwoj.whereishere.web;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.*;
import org.springframework.data.domain.*;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import twogtwoj.whereishere.domain.*;
import twogtwoj.whereishere.dto.*;
import twogtwoj.whereishere.file.FileStore;
import twogtwoj.whereishere.repository.ReviewPostRepository;
import twogtwoj.whereishere.service.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.MalformedURLException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewPostController {

    private final ReviewPostService reviewPostService;

    private final MemberService memberService;

    private final FileStore fileStore;

    private final ReviewLikeService reviewLikeService;

    private final ReviewPostRepository reviewPostRepository;

    @GetMapping("/post")
    public String reviewPostForm(@ModelAttribute ReviewPostDto reviewPostDto, Model model, @AuthenticationPrincipal User user) {
        Member member = memberService.findMemberByLoginId(user.getUsername());
        model.addAttribute("member", member);
        model.addAttribute("review",reviewPostDto);

        return "/review/reviewPost";
    }




    @RequestMapping(path = "/postPro", method = POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public String reviewPostPro(@ModelAttribute ReviewPostDto reviewPostDto, Model model, @AuthenticationPrincipal User user) throws Exception{
        Member member = memberService.findMemberByLoginId(user.getUsername());
        reviewPostService.post(reviewPostDto,member);

        model.addAttribute("message", "작성이 완료되었습니다.");
        model.addAttribute("searchUrl", "/review/list");

        return "/review/message";
    }

    @GetMapping("/view/{reviewPostId}")
    public String image(@PathVariable Long reviewPostId, Model model) {
        ReviewPost reviewPost = reviewPostService.reviewPostView(reviewPostId);
        model.addAttribute("image", reviewPost);
        return "review/view";
    }

    @ResponseBody
    @GetMapping("/images/{filename}")
    public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
        return new UrlResource("file:" + fileStore.getFullPath(filename));
    }

    @SneakyThrows
    @GetMapping("/list")
    public String reviewList(@PageableDefault(size = 7, sort = "reviewPostDate", direction = Sort.Direction.DESC) Pageable pageable,
                             @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
                             @RequestParam(name = "name", required = false) String name,
                             Model model) {

        Page<ReviewPost> list = null;

        if (searchKeyword == null && name == null) {
            list = reviewPostService.reviewPostList(pageable);
        } else if (searchKeyword != null && name != null) {
            list = reviewPostService.reviewPostSearchList(searchKeyword, name, pageable);
        } else if (searchKeyword != null) {
            list = reviewPostService.reviewPostSearchList(searchKeyword, null, pageable);
        } else if (name != null) {
            list = reviewPostService.reviewPostSearchList(null, name, pageable);
        } else {
            list = reviewPostService.reviewPostList(pageable);
        }

        //현재 페이지
        int nowPage = list.getPageable().getPageNumber() + 1; //list 변수 없이 사용해도 가능하지만 통일성을 위해서 사용한 것. 시작이 0이기에 +1 해줌
        //시작 페이지
        int startPage = Math.max(nowPage - 4, 1); // nowPage - 4 의 값이 1보다 작을 경우 1이 출력. 즉 두가지 중에 작은 값으로 출력하게 됨
        //마지막 페이지
        int endPage = Math.min(nowPage + 5, list.getTotalPages()); //nowPage + 5 이 값이 총 페이지 수 보다 클 경우 총 페이지 수가 출력됨

        if(endPage < startPage)
            endPage = nowPage;

        int totalCount = (int) list.getTotalElements(); // 총 게시물 수
        int searchCount = 0; // 검색 게시물 수

        if (searchKeyword != null || name != null) {
            if (list.getContent().size() > 0) {
                searchCount = (int) list.getTotalElements();
            }
        }


        model.addAttribute("list", list);
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("searchKeyword", searchKeyword);
        model.addAttribute("name", name);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("searchCount", searchCount);
        System.out.println(totalCount);
        System.out.println(searchCount);

        return "/review/reviewList";
    }

    @GetMapping("/myList")
    public String reviewMyList(Pageable pageable, Model model, @AuthenticationPrincipal User user) {
        Member member = memberService.findMemberByLoginId(user.getUsername());
        Page<ReviewPost> reviewPost = reviewPostService.reviewPostList(pageable);

        Page<ReviewPost> resultPage = PageableExecutionUtils.getPage(
                reviewPost.filter(n -> n.getMember().equals(member))
                        .stream().collect(Collectors.toList()), pageable,
                () -> reviewPost.filter(n -> n.getMember().equals(member)).stream().count());

        int nowPage = resultPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 5, resultPage.getTotalPages());

        if(endPage < startPage)
            endPage = nowPage;


        model.addAttribute("reviewPost", resultPage);
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        return "review/reviewMyList";
    }

    @GetMapping("/view")//localhost:8080/review/view?id=1
    public String review(@RequestParam Long reviewPostId, Model model, Principal principal,@AuthenticationPrincipal User user, HttpServletRequest request,
                         HttpServletResponse response) throws Exception {

        ReviewPost reviewPost = reviewPostService.reviewPostView(reviewPostId);

        model.addAttribute("review", reviewPost);


        List<ReviewLike> reviewLikeList = reviewLikeService.findAll();

        model.addAttribute("reviewLikeList",reviewLikeList);

        if(user.getAuthorities().stream().filter(n -> n.getAuthority().equals("ROLE_MEMBER")).toArray().length == 1){
            Member findMember = memberService.findMemberByLoginId(user.getUsername());
            model.addAttribute("member", findMember.getName());
        } else {
            model.addAttribute("member","");
        }

        try {
            Member member = memberService.findMemberByLoginId(principal.getName());
            int like = reviewPostService.findLike(reviewPostId, member.getId());
//
            // 로그인한 회원이 해당 후기 게시글에 좋아요를 누른 기록
            model.addAttribute("like", like);
            model.addAttribute("memberId", member.getId());
        } catch (ArrayIndexOutOfBoundsException e) {
            model.addAttribute("like", 0);
            model.addAttribute("memberId", 0);

        } finally {
            model.addAttribute("reviewPostId", reviewPost.getReviewPostId());
        }

        /*ReviewPost[] previousPost = reviewPostService.showPreviousPosts(reviewPostId);
        ReviewPost[] nextPost = reviewPostService.showNextPosts(reviewPostId);
        model.addAttribute("previousPost", previousPost);
        model.addAttribute("nextPost", nextPost);*/

       /* ReviewPost currentPost = reviewPostRepository.findById(reviewPostId).orElse(null);
        LocalDate currentPostReviewPostDate = currentPost != null ? currentPost.getReviewPostDate() : LocalDate.now();

        // 이전글 가져오기 (날짜순 정렬)
        Optional<ReviewPost> previousPost = reviewPostRepository.findPreviousPostByReviewPostDate(currentPostReviewPostDate);

        // 다음글 가져오기 (날짜순 정렬)
        Optional<ReviewPost> nextPost = reviewPostRepository.findNextPostByReviewPostDate(currentPostReviewPostDate);

        model.addAttribute("previousPost", previousPost);
        model.addAttribute("nextPost", nextPost);*/
    List<ReviewPost> adjacentPosts = reviewPostService.getAdjacentPostsByReviewPostDate(reviewPost.getReviewPostDate());

        /*ReviewPost previousPost = null;

        ReviewPost nextPost = null;

        if (!adjacentPosts.isEmpty()) {
            int index = -1;
            for (int i = 0; i < adjacentPosts.size(); i++) {
                if (adjacentPosts.get(i).getReviewPostDate().equals(reviewPost.getReviewPostDate())) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                previousPost = adjacentPosts.get(index - 1);
            }
            if (index < adjacentPosts.size() - 1) {
                nextPost = adjacentPosts.get(index + 1);
            } else {
                nextPost = null;
            }
        } else {
            previousPost = null;
            nextPost = null;
        }*/

        ReviewPost previousPost = null;
        ReviewPost nextPost = null;

        if (adjacentPosts.size() == 2) {
            previousPost = adjacentPosts.get(1);
            nextPost = adjacentPosts.get(0);
        }

        model.addAttribute("previousPost", previousPost);
        model.addAttribute("nextPost", nextPost);

        /* 조회수 로직 */

        HttpSession session = request.getSession();
        @SuppressWarnings("unchecked")
        List<Long> viewedIdList = (List<Long>) session.getAttribute("viewedIdList");
        if (viewedIdList == null) {
            viewedIdList = new ArrayList<>();
        }

        if (viewedIdList.contains(reviewPostId)) {
            // 이미 조회한 게시글인 경우
            model.addAttribute("viewCount", viewedIdList.size());
            model.addAttribute("viewedIdList", viewedIdList);
        } else {
            // 새로운 게시글을 조회한 경우
            reviewPostService.increaseViewCount(reviewPostId);
            viewedIdList.add(reviewPostId);
            session.setAttribute("viewedIdList", viewedIdList);
            model.addAttribute("viewCount", viewedIdList.size());
        }


        return "/review/view";
    }

    @ResponseBody
    @PostMapping("/like")
    public ReviewLikeResponseDto like(@RequestBody ReviewLikeDto reviewLikeDto) {

        Long memberId = reviewLikeDto.getMemberId();
        Long reviewPostId = reviewLikeDto.getReviewPostId();

        //저장 1, 삭제 0

        // 1. 자바스크립트 통해서 post 요청 보내기
        // 2. 컨트롤러에서 수신
        // 3. reviewPostService 내에서 "좋아요 증가" 로직 수행
        // 4. 증가하면 1, 감소하면 0

        return new ReviewLikeResponseDto(reviewPostService.saveLike(reviewPostId, memberId));
    }

    @GetMapping("/modify/{reviewPostId}")
    public String modify(@PathVariable Long reviewPostId, Model model) {

        model.addAttribute("review",reviewPostService.reviewPostView(reviewPostId));
        return "/review/reviewModify";
    }

    @RequestMapping(path = "/update/{reviewPostId}", method = POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public String update(@PathVariable("reviewPostId") Long reviewPostId,
                         @ModelAttribute ReviewPostDto reviewPostDto) throws Exception {

        reviewPostService.update(reviewPostId, reviewPostDto);

        return "redirect:/review/list";
    }

    @GetMapping("/delete/{reviewPostId}")
    public String delete(@PathVariable Long reviewPostId) {
        reviewPostService.delete(reviewPostId);
        return "redirect:/review/list";
    }
}