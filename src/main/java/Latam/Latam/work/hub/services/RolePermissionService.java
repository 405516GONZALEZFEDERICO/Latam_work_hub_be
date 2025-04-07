package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.entities.PermissionEntity;
import Latam.Latam.work.hub.entities.RoleEntity;

import java.util.List;
import java.util.Map;

public interface RolePermissionService {
    List<PermissionEntity> getPermissionsForRole(String roleName);
    void setRolePermissions(String roleName, List<String> permissionNames);
    void addPermissionToRole(String roleName, String permissionName);
    void removePermissionFromRole(String roleName, String permissionName);
    boolean hasPermission(String roleName, String permissionName);
    List<RoleEntity> getAllRoles();
    List<PermissionEntity> getAllPermissions();
    Map<String, List<String>> getAllRolePermissions();

    RoleEntity getRoleByName(String name);
}
