package com.bitirme.controller;

import com.bitirme.dto.user.RoleCreateRequest;
import com.bitirme.dto.user.RoleResponse;
import com.bitirme.dto.user.RoleUpdateRequest;
import com.bitirme.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Rol yönetimi için API endpoints")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @Operation(summary = "Yeni rol oluştur", description = "Sistemde yeni bir rol kaydı oluşturur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rol başarıyla oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek"),
            @ApiResponse(responseCode = "409", description = "Rol adı zaten kullanılıyor")
    })
    public ResponseEntity<RoleResponse> create(@RequestBody RoleCreateRequest request) {
        RoleResponse response = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Rol getir", description = "ID'ye göre rol bilgilerini getirir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol bulundu"),
            @ApiResponse(responseCode = "404", description = "Rol bulunamadı")
    })
    public ResponseEntity<RoleResponse> getById(@PathVariable Integer id) {
        RoleResponse response = roleService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm rolleri listele", description = "Sistemdeki tüm rolleri listeler")
    @ApiResponse(responseCode = "200", description = "Rol listesi başarıyla getirildi")
    public ResponseEntity<List<RoleResponse>> getAll() {
        List<RoleResponse> responses = roleService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping
    @Operation(summary = "Rol güncelle", description = "Mevcut bir rolün bilgilerini günceller. ID request body'den alınır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol başarıyla güncellendi"),
            @ApiResponse(responseCode = "404", description = "Rol bulunamadı"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    public ResponseEntity<RoleResponse> update(@RequestBody RoleUpdateRequest request) {
        RoleResponse response = roleService.update(request.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rol sil", description = "ID'ye göre rolü siler")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rol başarıyla silindi"),
            @ApiResponse(responseCode = "404", description = "Rol bulunamadı")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

