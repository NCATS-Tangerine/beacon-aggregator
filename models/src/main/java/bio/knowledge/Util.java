package bio.knowledge;

import java.util.List;

public interface Util {
	default Boolean nullOrEmpty(String query) {
		 return query==null || query.isEmpty();
	}
	default Boolean nullOrEmpty(List<?> query) {
		 return query==null || query.isEmpty();
	}
}
