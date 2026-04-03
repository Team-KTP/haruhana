package org.kwakmunsu.haruhana.domain.member.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.member.controller.dto.DeviceTokenSyncRequest;
import org.kwakmunsu.haruhana.domain.member.controller.dto.MemberCreateRequest;
import org.kwakmunsu.haruhana.domain.member.controller.dto.PreferenceAppendRequest;
import org.kwakmunsu.haruhana.domain.member.controller.dto.PreferenceUpdateRequest;
import org.kwakmunsu.haruhana.domain.member.controller.dto.ProfileUpdateRequest;
import org.kwakmunsu.haruhana.domain.member.service.MemberProfileResponse;
import org.kwakmunsu.haruhana.domain.member.service.MemberService;
import org.kwakmunsu.haruhana.global.annotation.LoginMember;
import org.kwakmunsu.haruhana.global.support.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
public class MemberController extends MemberDocsController {

    private final MemberService memberService;

    @Override
    @PostMapping("/v1/members/sign-up")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody @Valid MemberCreateRequest request) {
        Long memberId = memberService.createMember(request.toNewProfile(), request.toNewPreference());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberId));
    }

    @Override
    @PostMapping("/v1/members/preferences")
    public ResponseEntity<ApiResponse<?>> appendPreference(
            @RequestBody @Valid PreferenceAppendRequest request,
            @LoginMember Long memberId
    ) {

        memberService.appendPreference(request.toNewPreference(), memberId);

        return ResponseEntity.ok(ApiResponse.success());

    }

    @Override
    @PatchMapping("/v1/members/preferences")
    public ResponseEntity<ApiResponse<?>> updatePreference(
            @RequestBody @Valid PreferenceUpdateRequest request,
            @LoginMember Long memberId
    ) {
        memberService.updatePreference(request.toUpdatePreference(), memberId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @GetMapping("/v1/members")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getProfile(@LoginMember Long memberId) {
        MemberProfileResponse response = memberService.getProfile(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PatchMapping("/v1/members")
    public ResponseEntity<ApiResponse<?>> updateProfile(
            @RequestBody @Valid ProfileUpdateRequest request,
            @LoginMember Long memberId
    ) {
        memberService.updateProfile(request.toUpdateProfile(), memberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success());
    }

    @Override
    @DeleteMapping("/v1/members")
    public ResponseEntity<ApiResponse<?>> withdraw(@LoginMember Long memberId) {
        memberService.withdraw(memberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }

    @Override
    @PatchMapping("/v1/members/devices")
    public ResponseEntity<ApiResponse<?>> syncDevices(
            @RequestBody @Valid DeviceTokenSyncRequest request,
            @LoginMember Long memberId
    ) {
        memberService.syncDeviceTokens(memberId, request.deviceToken());

        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @DeleteMapping("/v1/members/devices")
    public ResponseEntity<ApiResponse<?>> deleteDevices(
            @RequestParam String deviceToken,
            @LoginMember Long memberId
    ) {
        memberService.deleteDeviceTokens(deviceToken, memberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }

    @Override
    @GetMapping("/v1/members/nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestParam @NotBlank String nickname) {
        boolean isAvailable = memberService.checkNicknameAvailable(nickname);

        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }

    @Override
    @GetMapping("/v1/members/login-id")
    public ResponseEntity<ApiResponse<Boolean>> checkLoginId(@RequestParam @NotBlank String loginId) {
        boolean isAvailable = memberService.checkLoginIdAvailable(loginId);

        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }

}