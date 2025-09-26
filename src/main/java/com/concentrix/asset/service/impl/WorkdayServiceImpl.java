package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.UserImportRequest;
import com.concentrix.asset.service.EmailService;
import com.concentrix.asset.service.UserService;
import com.concentrix.asset.service.WorkdayService;
import jakarta.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkdayServiceImpl implements WorkdayService {

    UserService userService;
    EmailService emailService;

    @NonFinal
    @Value("${app.path.upload.workday}")
    String workdayFolder;

    @NonFinal
    @Value("${app.notification.system-alert-email}")
    String alertSystemEmail;

    @Override
    public Map<String, Object> importFromWorkday() throws MessagingException {
        return resolveLatestCsv()
                .map(csv -> {
                    List<UserImportRequest> users = parseCsv(csv);
                    log.info("[WORKDAY][IMPORT] Parsed {} users from {}", users.size(), csv);
                    return userService.importUsers(users);
                })
                .orElseGet(() -> {
                    log.error("[WORKDAY][IMPORT] No Workday CSV found in folder: {}", workdayFolder);
                    return Map.of("status", "FAILED", "reason", "No CSV found");
                });
    }

    private Optional<Path> resolveLatestCsv() throws MessagingException {
        try {
            Path folder = Paths.get(workdayFolder);
            if (!Files.exists(folder)) {
                log.error("[WORKDAY][IMPORT] Workday folder does not exist: {}", workdayFolder);
                emailService.sendEmail(alertSystemEmail,
                        "Workday Import",
                        "Workday folder does not exist: " + workdayFolder,
                        null);
                return Optional.empty();
            }

            // Nếu workdayFolder là file CSV
            if (Files.isRegularFile(folder) && workdayFolder.toLowerCase().endsWith(".csv")) {
                return Optional.of(folder);
            }

            // Nếu workdayFolder là thư mục
            try (var files = Files.list(folder)) {
                return files.filter(f -> f.toString().toLowerCase().endsWith(".csv"))
                        .max(Comparator.comparingLong(f -> f.toFile().lastModified()));
            }

        } catch (IOException e) {
            log.error("[WORKDAY][IMPORT] Failed to resolve latest CSV: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private List<UserImportRequest> parseCsv(Path csvPath) {
        try (BufferedReader br = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return List.of();
            }

            String[] headers = splitSemicolon(headerLine);
            log.info("[WORKDAY][IMPORT] Headers: {}", Arrays.toString(headers));

            Map<String, Integer> idx = index(headers);

            return br.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> toUserRequest(line, idx))
                    .filter(req -> req.getEid() != null && req.getEmail() != null) // giữ record hợp lệ
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Workday CSV: " + e.getMessage(), e);
        }
    }

    private UserImportRequest toUserRequest(String line, Map<String, Integer> idx) {
        String[] cols = splitSemicolon(line);

        UserImportRequest req = new UserImportRequest();
        req.setEid(get(cols, findIndex(idx, "EMPLOYEE_NUMBER", "EMPLOYEE ID", "WORKER ID")));
        req.setFullName(get(cols, findIndex(idx, "FULL_NAME", "FULL NAME")));
        req.setJobTitle(get(cols, findIndex(idx, "JOB TITLE", "TITLE", "JOBTITLE")));
        String email = get(cols, findIndex(idx, "EMAIL WORK", "EMAIL", "EMAIL - WORK"));
        req.setEmail(email);
        req.setSso(email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : null);
        req.setMsa(get(cols, findIndex(idx, "MSA")));
        req.setMsaClient(get(cols, findIndex(idx, "MSA CLIENT", "CLIENT")));
        req.setCompany(get(cols, findIndex(idx, "COMPANY")));
        req.setCostCenter(get(cols, findIndex(idx, "COST CENTER ID", "COST CENTER")));
        req.setLocation(get(cols, findIndex(idx, "LOCATION NAME", "LOCATION")));
        req.setManagerEmail(get(cols, findIndex(idx, "SUPERVISOR EMAIL ID", "MANAGER EMAIL")));
        String status = get(cols, findIndex(idx, "EMPLOYEE STATUS", "STATUS", "EMPLOYEESTATUS"));
        req.setIsActive(status == null || status.equalsIgnoreCase("Active"));

        log.info("[WORKDAY][IMPORT] User: {}", req);
        return req;
    }

    // ================== CSV Helpers ==================

    private static Map<String, Integer> index(String[] headers) {
        return IntStream.range(0, headers.length)
                .boxed()
                .collect(Collectors.toMap(
                        i -> normalize(headers[i]),
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    private static int findIndex(Map<String, Integer> idx, String... keys) {
        for (String k : keys) {
            Integer i = idx.get(normalize(k));
            if (i != null) return i;
        }
        return -1;
    }

    private static String normalize(String header) {
        if (header == null) return "";
        return header
                .replace("\uFEFF", "") // remove BOM
                .trim()
                .toUpperCase()
                .replace("-", " ")
                .replace("_", " ");
    }

    private static String[] splitSemicolon(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ';' && !inQuotes) {
                parts.add(stripQuotes(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(stripQuotes(current.toString()));
        return parts.toArray(new String[0]);
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            return t.substring(1, t.length() - 1);
        }
        return t.isEmpty() ? null : t;
    }

    private static String get(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        return stripQuotes(cols[idx]);
    }
}
