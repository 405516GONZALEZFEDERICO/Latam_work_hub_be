package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.PermissionEntity;
import Latam.Latam.work.hub.entities.RoleEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.PermissionRepository;
import Latam.Latam.work.hub.repositories.RoleRepository;
import Latam.Latam.work.hub.services.RolePermissionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<PermissionEntity> getPermissionsForRole(String roleName) {
        RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));
        return new ArrayList<>(role.getPermissions());
    }

    @Override
    @Transactional
    public void setRolePermissions(String roleName, List<String> permissionNames) {
        RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));

        List<PermissionEntity> permisos = permissionRepository.findAll()
                .stream()
                .filter(p -> permissionNames.contains(p.getName()))
                .collect(Collectors.toList());

        role.setPermissions(new HashSet<>(permisos));
        roleRepository.save(role);

        log.info("Actualizados {} permisos para el rol {}", permisos.size(), roleName);
    }

    @Override
    @Transactional
    public void addPermissionToRole(String roleName, String permissionName) {
        RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));
        PermissionEntity permiso = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + permissionName));

        role.getPermissions().add(permiso);
        roleRepository.save(role);

        log.info("Añadido permiso {} al rol {}", permissionName, roleName);
    }

    @Override
    @Transactional
    public void removePermissionFromRole(String roleName, String permissionName) {
        RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));

        role.getPermissions().removeIf(p -> p.getName().equals(permissionName));
        roleRepository.save(role);

        log.info("Eliminado permiso {} del rol {}", permissionName, roleName);
    }

    @Override
    public boolean hasPermission(String roleName, String permissionName) {
        return roleRepository.findByName(roleName)
                .map(role -> role.getPermissions().stream()
                        .anyMatch(p -> p.getName().equals(permissionName)))
                .orElse(false);
    }

    @Override
    public List<RoleEntity> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public List<PermissionEntity> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Map<String, List<String>> getAllRolePermissions() {
        return roleRepository.findAll().stream()
                .collect(Collectors.toMap(
                        RoleEntity::getName,
                        role -> role.getPermissions().stream()
                                .map(PermissionEntity::getName)
                                .collect(Collectors.toList())
                ));
    }

    @Override
    public RoleEntity getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new AuthException("Rol no encontrado: " + name));
    }
}
