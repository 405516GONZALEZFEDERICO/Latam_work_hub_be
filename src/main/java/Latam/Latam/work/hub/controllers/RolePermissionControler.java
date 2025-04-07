package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.entities.PermissionEntity;
import Latam.Latam.work.hub.entities.RoleEntity;
import Latam.Latam.work.hub.services.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Slf4j
public class RolePermissionControler {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    public ResponseEntity<List<RoleEntity>> getAllRoles() {
        return ResponseEntity.ok(rolePermissionService.getAllRoles());
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionEntity>> getAllPermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllPermissions());
    }

    @GetMapping("/{roleName}/permissions")
    public ResponseEntity<List<PermissionEntity>> getPermissionsForRole(@PathVariable String roleName) {
        return ResponseEntity.ok(rolePermissionService.getPermissionsForRole(roleName));
    }

    @PostMapping("/{roleName}/permissions")
    public ResponseEntity<String> setRolePermissions(
            @PathVariable String roleName,
            @RequestBody List<String> permissionNames
    ) {
        rolePermissionService.setRolePermissions(roleName, permissionNames);
        return ResponseEntity.ok("Permisos establecidos correctamente para el rol: " + roleName);
    }

    @PostMapping("/{roleName}/permissions/add")
    public ResponseEntity<String> addPermissionToRole(
            @PathVariable String roleName,
            @RequestParam String permissionName
    ) {
        rolePermissionService.addPermissionToRole(roleName, permissionName);
        return ResponseEntity.ok("Permiso añadido correctamente.");
    }

    @DeleteMapping("/{roleName}/permissions/remove")
    public ResponseEntity<String> removePermissionFromRole(
            @PathVariable String roleName,
            @RequestParam String permissionName
    ) {
        rolePermissionService.removePermissionFromRole(roleName, permissionName);
        return ResponseEntity.ok("Permiso eliminado correctamente.");
    }

    @GetMapping("/{roleName}/has-permission")
    public ResponseEntity<Boolean> hasPermission(
            @PathVariable String roleName,
            @RequestParam String permissionName
    ) {
        boolean result = rolePermissionService.hasPermission(roleName, permissionName);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/map")
    public ResponseEntity<Map<String, List<String>>> getAllRolePermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllRolePermissions());
    }
}
