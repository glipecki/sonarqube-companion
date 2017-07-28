package pl.consdata.ico.sqcompanion.violations;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class ViolationsHistory {

	@Singular
	private List<ViolationHistoryEntry> violationHistoryEntries;

}
