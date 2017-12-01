import { Component, OnInit } from '@angular/core';

declare var $: JQueryStatic;

import * as _ from 'lodash';
import * as backbone from 'backbone';
import * as joint from 'jointjs';

import { OperatorDragDropService } from '../../service/operator-drag-drop/operator-drag-drop.service';
import { WorkflowModelService } from '../../service/workflow-graph/workflow-model.service';
import { WorkflowUIChangeService } from '../../service/workflow-graph/workflow-ui-change.service';


@Component({
  selector: 'texera-workflow-editor',
  templateUrl: './workflow-editor.component.html',
  styleUrls: ['./workflow-editor.component.scss']
})
export class WorkflowEditorComponent implements OnInit {

  // the id in HTML must also be changed whenever this is changed
  readonly WORKFLOW_EDITOR_ID = 'texera-workflow-holder';
  private paper: joint.dia.Paper;

  constructor(
    private workflowModelSerivce: WorkflowModelService,
    private workflowUIChangeService: WorkflowUIChangeService,
    private operatorDragDropService: OperatorDragDropService) {

  }

  ngOnInit() {
    this.paper = new joint.dia.Paper({
      el: $('#' + this.WORKFLOW_EDITOR_ID),
      width: 600,
      height: 200,
      model: this.workflowModelSerivce.uiGraph,
      gridSize: 1,
      snapLinks: true,
      linkPinning: false,
      validateConnection: function (sourceView, sourceMagnet, targetView, targetMagnet) {
        return sourceMagnet !== targetMagnet;
      }
    });

    this.operatorDragDropService.registerDrop(this.WORKFLOW_EDITOR_ID);

    this.workflowModelSerivce.registerWorkflowPaper(this.paper);

    this.paper.on('cell:pointerdown', (cellView, evt, x, y) => {
      this.workflowUIChangeService.selectOperator(cellView.model.id);
    });
  }


}
