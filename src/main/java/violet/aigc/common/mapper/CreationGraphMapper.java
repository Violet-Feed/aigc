package violet.aigc.common.mapper;

import violet.aigc.common.pojo.Creation;

import java.util.List;

public interface CreationGraphMapper {
    List<Creation> getCreationsByUser(Long userId, Integer page);

    List<Creation> getCreationsByDigg(Long userId, Integer page);

    List<Creation> getCreationsByFriend(Long userId, Integer page);
}
