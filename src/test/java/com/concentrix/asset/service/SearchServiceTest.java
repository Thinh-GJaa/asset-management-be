package com.concentrix.asset.service;

import com.concentrix.asset.dto.response.DeviceResponse;
import com.concentrix.asset.dto.response.SearchResultResponse;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.entity.Device;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.mapper.DeviceMapper;
import com.concentrix.asset.mapper.UserMapper;
import com.concentrix.asset.repository.DeviceRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private SearchServiceImpl searchService;

    private User userByEmail;
    private User userByEid;
    private User userByFullName;
    private User userBySso;
    private User userByMsa;
    private User userWithNulls;

    private Device device1;
    private Device device2NullSerial;

    // No prebuilt stubs needed; mapping verified via answers

    @BeforeEach
    void setUp() {
        userByEmail = new User();
        userByEmail.setEmail("john.doe@company.com");

        userByEid = new User();
        userByEid.setEid("EID123");

        userByFullName = new User();
        userByFullName.setFullName("Jane SMITH");

        userBySso = new User();
        userBySso.setSso("sso-abc-999");

        userByMsa = new User();
        userByMsa.setMsa("MSA-777");

        userWithNulls = new User();
        // all matchable fields null

        device1 = new Device();
        device1.setSerialNumber("SN-ABC-001");

        device2NullSerial = new Device();
        device2NullSerial.setSerialNumber(null);
    }

    @Test
    void search_nullQuery_returnsEmpty() {
        SearchResultResponse res = searchService.search(null);
        assertNotNull(res);
        assertEquals(0, res.getTotal());
        assertTrue(res.getUsers().isEmpty());
        assertTrue(res.getDevices().isEmpty());
        verifyNoInteractions(userRepository, deviceRepository, userMapper, deviceMapper);
    }

    @Test
    void search_blankQuery_returnsEmpty() {
        SearchResultResponse res = searchService.search("   \t  ");
        assertNotNull(res);
        assertEquals(0, res.getTotal());
        assertTrue(res.getUsers().isEmpty());
        assertTrue(res.getDevices().isEmpty());
        verifyNoInteractions(userRepository, deviceRepository, userMapper, deviceMapper);
    }

    @Test
    void search_matchesUsersAcrossAllFields_andDevices_caseInsensitive() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(
            userByEmail, userByEid, userByFullName, userBySso, userByMsa, userWithNulls
        ));
        when(deviceRepository.findAll()).thenReturn(Arrays.asList(device1, device2NullSerial));

        when(userMapper.toUserResponse(any(User.class)))
            .thenAnswer(invocation -> UserResponse.builder().email(((User) invocation.getArgument(0)).getEmail()).eid(((User) invocation.getArgument(0)).getEid()).fullName(((User) invocation.getArgument(0)).getFullName()).sso(((User) invocation.getArgument(0)).getSso()).msa(((User) invocation.getArgument(0)).getMsa()).build());
        when(deviceMapper.toDeviceResponse(any(Device.class)))
            .thenAnswer(invocation -> DeviceResponse.builder().serialNumber(((Device) invocation.getArgument(0)).getSerialNumber()).build());

        // Query fragments that hit different fields (lower/upper case mix)
        SearchResultResponse byEmail = searchService.search("DOE@COMP");
        assertTrue(byEmail.getTotal() >= 1);
        assertTrue(byEmail.getUsers().stream().anyMatch(u -> "john.doe@company.com".equalsIgnoreCase(u.getEmail())));

        SearchResultResponse byEid = searchService.search("eid12");
        assertTrue(byEid.getUsers().stream().anyMatch(u -> "EID123".equalsIgnoreCase(u.getEid())));

        SearchResultResponse byFullName = searchService.search("jane s");
        assertTrue(byFullName.getUsers().stream().anyMatch(u -> "Jane SMITH".equalsIgnoreCase(u.getFullName())));

        SearchResultResponse bySso = searchService.search("SSO-ABC");
        assertTrue(bySso.getUsers().stream().anyMatch(u -> "sso-abc-999".equalsIgnoreCase(u.getSso())));

        SearchResultResponse byMsa = searchService.search("msa-77");
        assertTrue(byMsa.getUsers().stream().anyMatch(u -> "MSA-777".equalsIgnoreCase(u.getMsa())));

        SearchResultResponse byDeviceSerial = searchService.search("abc-001");
        assertTrue(byDeviceSerial.getDevices().stream().anyMatch(d -> "SN-ABC-001".equalsIgnoreCase(d.getSerialNumber())));

        // Ensure null-only user and null-serial device don't cause NPE and aren't matched accidentally
        SearchResultResponse noneMatchNulls = searchService.search("not-exist");
        assertEquals(0, noneMatchNulls.getTotal());

        verify(userRepository, atLeastOnce()).findAll();
        verify(deviceRepository, atLeastOnce()).findAll();
    }

    @Test
    void search_handlesNullFieldsSafely_andDoesNotMatchNulls() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(userWithNulls));
        when(deviceRepository.findAll()).thenReturn(Arrays.asList(device2NullSerial));

        SearchResultResponse res = searchService.search("x");
        assertNotNull(res);
        assertEquals(0, res.getTotal());
        assertTrue(res.getUsers().isEmpty());
        assertTrue(res.getDevices().isEmpty());
        verify(userMapper, never()).toUserResponse(any());
        verify(deviceMapper, never()).toDeviceResponse(any());
    }

    @Test
    void search_noMatches_returnsEmpty_andMapperNotCalled() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(userByEmail));
        when(deviceRepository.findAll()).thenReturn(Arrays.asList(device1));

        SearchResultResponse res = searchService.search("zzz-not-found");
        assertNotNull(res);
        assertEquals(0, res.getTotal());
        assertTrue(res.getUsers().isEmpty());
        assertTrue(res.getDevices().isEmpty());
        verify(userMapper, never()).toUserResponse(any());
        verify(deviceMapper, never()).toDeviceResponse(any());
    }
}


