package org.kwakmunsu.haruhana;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kwakmunsu.haruhana.admin.category.controller.AdminCategoryController;
import org.kwakmunsu.haruhana.admin.category.service.AdminCategoryService;
import org.kwakmunsu.haruhana.admin.member.controller.AdminMemberController;
import org.kwakmunsu.haruhana.admin.member.service.AdminMemberService;
import org.kwakmunsu.haruhana.admin.problem.controller.AdminProblemController;
import org.kwakmunsu.haruhana.admin.problem.service.AdminProblemService;
import org.kwakmunsu.haruhana.admin.statistics.controller.StatisticsController;
import org.kwakmunsu.haruhana.admin.statistics.service.StatisticsService;
import org.kwakmunsu.haruhana.domain.auth.controller.AuthController;
import org.kwakmunsu.haruhana.domain.auth.service.AuthService;
import org.kwakmunsu.haruhana.domain.category.controller.CategoryController;
import org.kwakmunsu.haruhana.domain.category.service.CategoryService;
import org.kwakmunsu.haruhana.domain.dailyproblem.controller.DailyProblemController;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.DailyProblemService;
import org.kwakmunsu.haruhana.domain.member.controller.MemberController;
import org.kwakmunsu.haruhana.domain.member.service.MemberService;
import org.kwakmunsu.haruhana.domain.storage.controller.StorageController;
import org.kwakmunsu.haruhana.domain.storage.service.StorageService;
import org.kwakmunsu.haruhana.domain.streak.controller.StreakController;
import org.kwakmunsu.haruhana.domain.streak.service.StreakService;
import org.kwakmunsu.haruhana.domain.submission.controller.SubmissionController;
import org.kwakmunsu.haruhana.domain.submission.service.FeedbackGradingService;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionService;
import org.kwakmunsu.haruhana.global.support.notification.ErrorNotificationSender;
import org.kwakmunsu.haruhana.security.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@ActiveProfiles("test")
@Import({TestSecurityConfig.class, MethodValidationPostProcessor.class})
@WebMvcTest(
        controllers = {
                AdminMemberController.class,
                AdminProblemController.class,
                MemberController.class,
                AuthController.class,
                DailyProblemController.class,
                StreakController.class,
                AdminCategoryController.class,
                CategoryController.class,
                StorageController.class,
                StatisticsController.class,
                SubmissionController.class
        })
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvcTester mvcTester;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected AdminMemberService adminMemberService;

    @MockitoBean
    protected AdminProblemService adminProblemService;

    @MockitoBean
    protected MemberService memberService;

    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected DailyProblemService dailyProblemService;

    @MockitoBean
    protected SubmissionService submissionService;

    @MockitoBean
    protected StreakService streakService;

    @MockitoBean
    protected AdminCategoryService adminCategoryService;

    @MockitoBean
    protected CategoryService categoryService;

    @MockitoBean
    protected StorageService storageService;

    @MockitoBean
    protected StatisticsService statisticsService;

    @MockitoBean
    protected FeedbackGradingService feedbackGradingService;

    @MockitoBean
    protected ErrorNotificationSender errorNotificationSender;

}