package com.storyweaver.security;

import com.storyweaver.domain.entity.User;
import com.storyweaver.service.UserService;
import com.storyweaver.utils.JwtUtil;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private static final String TOKEN = "scene-token";

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(buildJwtUtilStub(), buildUserServiceStub());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateAsyncDispatchWithBearerToken() throws Exception {
        MockHttpServletRequest request = buildAuthenticatedRequest(DispatcherType.ASYNC);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> {
            chainInvoked.set(true);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertAuthenticatedSceneUser(authentication);
        });

        assertTrue(chainInvoked.get());
    }

    @Test
    void shouldAuthenticateErrorDispatchWithBearerToken() throws Exception {
        MockHttpServletRequest request = buildAuthenticatedRequest(DispatcherType.ERROR);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> {
            chainInvoked.set(true);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertAuthenticatedSceneUser(authentication);
        });

        assertTrue(chainInvoked.get());
    }

    private MockHttpServletRequest buildAuthenticatedRequest(DispatcherType dispatcherType) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(dispatcherType);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private void assertAuthenticatedSceneUser(Authentication authentication) {
        assertNotNull(authentication);
        CurrentUser principal = (CurrentUser) authentication.getPrincipal();
        assertEquals(7L, principal.userId());
        assertEquals("scene-user", principal.username());
    }

    private JwtUtil buildJwtUtilStub() {
        return new JwtUtil() {
            @Override
            public String getUsernameFromToken(String token) {
                return TOKEN.equals(token) ? "scene-user" : null;
            }

            @Override
            public Long getUserIdFromToken(String token) {
                return TOKEN.equals(token) ? 7L : null;
            }

            @Override
            public Boolean isTokenExpired(String token) {
                return false;
            }
        };
    }

    private UserService buildUserServiceStub() {
        User sceneUser = new User();
        sceneUser.setId(7L);
        sceneUser.setUsername("scene-user");
        sceneUser.setRoleCode("USER");
        sceneUser.setStatus(1);
        sceneUser.setDeleted(0);

        return (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class<?>[]{UserService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> sceneUser;
                    case "isAdmin" -> false;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "UserServiceTestStub";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
