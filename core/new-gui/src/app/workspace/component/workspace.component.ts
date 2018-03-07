import { Component, OnInit, ViewChild } from '@angular/core';

import { NavigationComponent } from './navigation/navigation.component';
import { OperatorViewComponent } from './operator-view/operator-view.component';
import { WorkflowEditorComponent } from './workflow-editor/workflow-editor.component';
import { ResultViewComponent } from './result-view/result-view.component';
import { PropertyEditorComponent } from './property-editor/property-editor.component';

import { OperatorMetadataService } from '../service/operator-metadata/operator-metadata.service';

import { WorkflowTexeraGraphService } from '../service/workflow-graph/model/workflow-texera-graph.service';
import { WorkflowJointGraphService } from '../service/workflow-graph/model/workflow-joint-graph.service';
import { WorkflowModelActionService } from '../service/workflow-graph/model-action/workflow-model-action.service';
import { WorkflowModelEventService } from '../service/workflow-graph/model-event/workflow-model-event.service';
import { WorkflowGraphUtilsService } from '../service/workflow-graph/utils/workflow-graph-utils.service';
import { WorkflowViewEventService } from '../service/workflow-graph/view-event/workflow-view-event.service';
import { WorkflowViewObserverService } from '../service/workflow-graph/view-observer/workflow-view-observer.service';

import { ExecuteWorkflowService } from '../service/execute-workflow/execute-workflow.service';
import { OperatorDragDropService } from '../service/operator-drag-drop/operator-drag-drop.service';
import { OperatorUIElementService } from '../service/operator-ui-element/operator-ui-element.service';



@Component({
  selector: 'texera-workspace',
  templateUrl: './workspace.component.html',
  styleUrls: ['./workspace.component.scss'],
  providers: [

    OperatorMetadataService,

    WorkflowTexeraGraphService,
    WorkflowJointGraphService,
    WorkflowModelActionService,
    WorkflowModelEventService,
    WorkflowGraphUtilsService,
    WorkflowViewEventService,
    WorkflowViewObserverService,

    ExecuteWorkflowService,
    OperatorDragDropService,
    OperatorUIElementService

  ]
})
export class WorkspaceComponent implements OnInit {

/**
 * Use all the services here.
 * Because the stupid Angular Dependency Injector won't initialize the
 *   service if it's not directly used by someone else.
 * But I just want Angular to initialize all my singleton services!
 */
  constructor(
    private operatorMetadataService: OperatorMetadataService,
    private workflowTexeraGraphService: WorkflowTexeraGraphService,
    private workflowJointGraphService: WorkflowJointGraphService,
    private workflowModelActionService: WorkflowModelActionService,
    private workflowModelEventService: WorkflowModelEventService,
    private workflowGraphUtilsService: WorkflowGraphUtilsService,
    private workflowViewEventService: WorkflowViewEventService,
    private workflowViewObserverService: WorkflowViewObserverService,
    private executeWorkflowService: ExecuteWorkflowService,
    private operatorDragDropService: OperatorDragDropService,
    private operatorUIElementService: OperatorUIElementService,
  ) { }

  ngOnInit() {
  }

}
