import { Injectable, ElementRef } from '@angular/core';

import { Observable, Subject } from 'rxjs/Rx';

import { WorkflowDataService } from '../current-workflow/workflow-data.service';
import { OperatorUIElementService } from '../operator-ui-element/operator-ui-element.service';

import * as joint from 'jointjs';

@Injectable()
export class OperatorDragDropService {

  private currentOperatorType = '';

  constructor(
    private workflowDataService: WorkflowDataService,
    private operatorUIElementService: OperatorUIElementService) { }

  createNewOperatorUIElement(operatorType: string) {
    this.currentOperatorType = operatorType;

    // create a temporary ghost element
    jQuery('body').append('<div id="flyPaper" style="position:fixed;z-index:100;;pointer-event:none;"></div>');

    // get the UI element of the operator
    const operatorUIElement = this.operatorUIElementService.getOperatorUIElement(operatorType);

    // create the jointjs model and paper of the ghost element
    const tempGhostModel = new joint.dia.Graph();

    const tempGhostPaper = new joint.dia.Paper({
      el: jQuery('#flyPaper'),
      width: 100,
      height: 30,
      model: tempGhostModel,
      gridSize: 1
    });


    tempGhostModel.addCell(operatorUIElement);

    return jQuery('#flyPaper');
  }

  onOperatorDrop(event: Event, ui: JQueryUI.DroppableEventUIParam) {
    this.workflowDataService.addOperator(ui.offset.left, ui.offset.top, this.currentOperatorType);
  }

}
