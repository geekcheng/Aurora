package me.aurora.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import me.aurora.domain.Permission;
import me.aurora.domain.Role;
import me.aurora.repository.RoleRepo;
import me.aurora.repository.spec.RoleSpec;
import me.aurora.service.RoleService;
import me.aurora.service.dto.RoleDTO;
import me.aurora.service.mapper.RoleMapper;
import me.aurora.util.PageUtil;
import me.aurora.config.exception.AuroraException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * @author 郑杰
 * @date 2018/08/23 17:27:03
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public List<Map<String, Object>> buildRoleTree(List<Role> roles) {
        List<Map<String,Object>> maps = new LinkedList<>();
        for (Role role:roles) {
            if (role!=null){
                Map<String,Object> map = new HashMap<>(16);
                map.put("id",role.getId());
                map.put("name",role.getName());
                maps.add(map);
            }
        }
        return maps;
    }

    @Override
    public List<Role> getAllRole() {
        return roleRepo.findAll();
    }

    @Override
    public Map getRoleInfo(RoleSpec roleSpec, Pageable pageable) {
        Page<Role> roles = roleRepo.findAll(roleSpec,pageable);
        Page<RoleDTO> roleDTOS = roles.map(roleMapper::toDto);
        return PageUtil.buildPage(roleDTOS.getContent(),roles.getTotalElements());
    }

    @Override
    public void inster(Role role, String permissions) {
        if(StrUtil.hasEmpty(role.getName())){
            throw new AuroraException(HttpStatus.HTTP_NOT_FOUND,"角色名称不能为空");
        }
        if(roleRepo.findByName(role.getName())!=null){
            throw new AuroraException(HttpStatus.HTTP_BAD_REQUEST,"角色已存在");
        }
        role.setPermissions(getPermissions(permissions));
        roleRepo.save(role);
    }

    @Override
    public Role findById(Long id) {
        return roleRepo.findById(id).get();
    }

    @Override
    public void update(Role role, String permissions) {
        if(role.getId() == null){
            throw new AuroraException(HttpStatus.HTTP_NOT_FOUND,"角色ID不能为空");
        }
        if(StrUtil.hasEmpty(role.getName())){
            throw new AuroraException(HttpStatus.HTTP_NOT_FOUND,"角色名称不能为空");
        }
        Role old = roleRepo.findByName(role.getName());
        if(old!=null && !role.getId().equals(old.getId())){
            throw new AuroraException(HttpStatus.HTTP_BAD_REQUEST,"角色已存在");
        }
        old.setName(role.getName());
        old.setRemark(role.getRemark());
        old.setPermissions(getPermissions(permissions));
        roleRepo.save(old);
    }

    @Override
    public void delete(Long id) {
        Role role = roleRepo.findById(id).get();
        if(role == null){
            throw new AuroraException(404,"ID为"+id+"的角色未找到");
        }
        if(role.getUsers() != null && role.getUsers().size() != 0){
            throw new AuroraException(400,"角色使用中，请先解绑用户");
        }
        roleRepo.deleteById(id);
    }

    @Override
    public String getPermissions(Set<Permission> permissions) {
        String str = "";
        for (Permission permission : permissions) {
            str += permission.getId()+",";
        }
        return StrUtil.hasEmpty(str)?"":str.substring(0,str.length() - 1);
    }

    /**
     * 获取权限集合
     * @param permissions
     * @return
     */
    public Set<Permission> getPermissions(String permissions){
        Set<Permission> permissionSet = new HashSet<>();
        for(String permissionId:permissions.split(",")){
            Permission permission = new Permission();
            permission.setId(Long.parseLong(permissionId));
            permissionSet.add(permission);
        }
        return permissionSet;
    }

    @Override
    public String getRoles(Set<Role> roles) {
        String str = "";
        for (Role role : roles) {
            str += role.getId()+",";
        }
        return StrUtil.hasEmpty(str)?"":str.substring(0,str.length() - 1);
    }
}
