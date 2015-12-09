/**
 *
 */

package to.rtc.cli.migrate.git;

import java.util.Iterator;
import java.util.List;

import com.ibm.team.filesystem.cli.core.subcommands.IScmClientConfiguration;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogBaselineEntryDTO;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogChangeSetEntryDTO;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogComponentEntryDTO;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogDirectionEntryDTO;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogEntryDTO;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogOslcLinkEntryDTO;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogVersionableEntryDTO;
import com.ibm.team.filesystem.common.internal.rest.client.changelog.ChangeLogWorkItemEntryDTO;
import com.ibm.team.filesystem.rcp.core.internal.changelog.BaseChangeLogEntryVisitor;
import com.ibm.team.filesystem.rcp.core.internal.changelog.IChangeLogOutput;
import com.ibm.team.rtc.cli.infrastructure.internal.core.CLIClientException;

/**
 * @author florian.buehlmann
 *
 */
public class ChangeLogEntryVisitor extends BaseChangeLogEntryVisitor {

  private ChangeLogEntryDTO oldBaseline;
  private IScmClientConfiguration config;
  private String workspace;
  private boolean initialLoadDone = false;

  private void createGitTag() {
    // TODO Auto-generated method stub
  }

  private void commitGitChanges() {
    // TODO Auto-generated method stub
  }

  private void acceptAndLoadBaseline(IScmClientConfiguration config2, String workspace2, String baselineItemId) throws CLIClientException {
    AcceptCommandDelegate.runAcceptBaseline(config, workspace, baselineItemId);
    handleInitialLoad();
  }

  private void acceptAndLoadChangeset(IScmClientConfiguration config2, String workspace2, String changeSetUuid) throws CLIClientException {
    AcceptCommandDelegate.runAcceptChangeSet(config, workspace, changeSetUuid);
    handleInitialLoad();
  }

  public void init() {
    handleInitialLoad();
    initialLoadDone = false;
  }

  private void handleInitialLoad() {
    if (!initialLoadDone) {
      try {
        LoadCommandDelegate.runLoad(config, workspace, true);
        initialLoadDone = true;
      } catch (CLIClientException e) {
        throw new RuntimeException("Not a valid sandbox. Please run [scm load] before [scm migrate-to-git] command");
      }
    }
  }

  public ChangeLogEntryVisitor(IChangeLogOutput out, IScmClientConfiguration config, String workspace) {
    this.config = config;
    this.workspace = workspace;
    setOutput(out);
  }

  public void acceptInto(ChangeLogEntryDTO root) {
    if (!enter(root)) return;
    for (Iterator<?> iterator = root.getChildEntries().iterator(); iterator.hasNext();) {
      ChangeLogEntryDTO child = (ChangeLogEntryDTO)iterator.next();
      visitChild(root, child);
      acceptInto(child);
    }

    exit(root);
  }

  @Override
  protected void visitChangeSet(ChangeLogEntryDTO parent, ChangeLogChangeSetEntryDTO dto) {
    String workItemText = "";
    List<?> workItems = dto.getWorkItems();
    if (workItems != null && !workItems.isEmpty()) {
      final ChangeLogWorkItemEntryDTO workItem = (ChangeLogWorkItemEntryDTO)workItems.get(0);
      workItemText = workItem.getWorkItemNumber() + ": " + workItem.getEntryName();
      if (workItemText.length() > 10) {
        workItemText = workItemText.substring(0, 10);
      }
    }
    final String changeSetUuid = dto.getItemId();
    System.out.println(handleBaselineChange(parent) + " [" + changeSetUuid + "], Story [" + workItemText + "] Comment ["
        + dto.getEntryName() + "] User [" + dto.getCreator().getFullName() + "]");
    try {
      acceptAndLoadChangeset(config, workspace, changeSetUuid);
      commitGitChanges();
    } catch (CLIClientException e) {
      e.printStackTrace();
    }
  }

  private String handleBaselineChange(ChangeLogEntryDTO parent) {
    if (oldBaseline != null && !parent.getItemId().equals(oldBaseline.getItemId())) {
      if ("clentry_baseline".equals(oldBaseline.getEntryType())) {
        ChangeLogBaselineEntryDTO baseline = (ChangeLogBaselineEntryDTO)oldBaseline;
        System.out.println("Baseline [" + baseline.getBaselineId() + ":" + baseline.getEntryName() + "] must be created!");
        // Accept baseline to target workspace
        try {
          createGitTag();
          acceptAndLoadBaseline(config, workspace, baseline.getItemId());
        } catch (CLIClientException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      oldBaseline = parent;
    }

    if ("clentry_baseline".equals(parent.getEntryType())) {
      ChangeLogBaselineEntryDTO baseline = (ChangeLogBaselineEntryDTO)parent;
      oldBaseline = baseline;
      return baseline.getBaselineId() + ":" + baseline.getEntryName() + " --> ";
    } else {
      return " NO BASELINE --> ";
    }
  }

  @Override
  protected void visitBaseline(ChangeLogEntryDTO parent, ChangeLogBaselineEntryDTO dto) {
  }

  @Override
  protected void visitComponent(ChangeLogEntryDTO parent, ChangeLogComponentEntryDTO dto) {
  }

  @Override
  protected void visitDirection(ChangeLogEntryDTO parent, ChangeLogDirectionEntryDTO dto) {
  }

  @Override
  protected void visitOslcLink(ChangeLogEntryDTO parent, ChangeLogOslcLinkEntryDTO dto) {
  }

  @Override
  protected void visitVersionable(ChangeLogEntryDTO parent, ChangeLogVersionableEntryDTO dto) {
  }

  @Override
  protected void visitWorkItem(ChangeLogEntryDTO parent, ChangeLogWorkItemEntryDTO dto, boolean inChangeSet) {
  }

}
