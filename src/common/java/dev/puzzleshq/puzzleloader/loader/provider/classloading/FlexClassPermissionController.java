package dev.puzzleshq.puzzleloader.loader.provider.classloading;

import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlexClassPermissionController {

    final Map<String, PermissionCollection> collection = new ConcurrentHashMap<>();

    public void addPermission(String pkg, Permission permission) {
        PermissionCollection permissionCollection = collection.get(pkg);
        if (permissionCollection == null) permissionCollection = new Permissions();

        permissionCollection.add(permission);
        collection.put(pkg, permissionCollection);
    }

    public void addPermission(String pkg, Permission... permissions) {
        PermissionCollection permissionCollection = collection.get(pkg);
        if (permissionCollection == null) permissionCollection = new Permissions();

        for (Permission perm : permissions) permissionCollection.add(perm);

        collection.put(pkg, permissionCollection);
    }

    public PermissionCollection getPermissions(String pkg) {
        PermissionCollection permissionCollection = collection.get(pkg);
        if (permissionCollection == null) {
            permissionCollection = new Permissions();
            permissionCollection.add(new AllPermission());
            collection.put(pkg, permissionCollection);
        }

        return permissionCollection;
    }

}
