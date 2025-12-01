package violet.aigc.common.mapper;

import java.util.List;

public interface CreationGraphMapper {
    void createCreation(Long userId, Long creationId);

    List<Long> getCreationIdsByUser(Long userId, Integer page);

    List<Long> getCreationIdsByFriend(Long userId, Integer page);
}
