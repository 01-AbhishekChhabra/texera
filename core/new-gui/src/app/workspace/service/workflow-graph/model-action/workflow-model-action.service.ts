import { Injectable } from '@angular/core';
import { WorkflowModelService } from '../model/workflow-model.service';
import { OperatorUIElementService } from '../../operator-ui-element/operator-ui-element.service';
import { WorkflowModelEventService } from '../model-event/workflow-model-event.service';

@Injectable()
export class WorkflowModelActionService {

  constructor(
    private workflowModelService: WorkflowModelService,
    private workflowModelEventService: WorkflowModelEventService,
    private operatorUIElementService: OperatorUIElementService
  ) { }

  public addOperator(operator: OperatorPredicate, xOffset: number, yOffset: number): void {
    // get operaotr UI element
    const operatorUIElement = this.operatorUIElementService.getOperatorUIElement(
      operator.operatorType, operator.operatorID);
    // change its position
    operatorUIElement.position(
      xOffset - this.workflowModelService.uiPaper.pageOffset().x,
      yOffset - this.workflowModelService.uiPaper.pageOffset().y);

    // add the operator UI element to the UI model
    this.workflowModelService.uiGraph.addCell(operatorUIElement);
  }

  public deleteOperator(operatorID: string): void {
    this.workflowModelService.uiGraph.getCell(operatorID).remove();
  }

  public changeOperatorProperty(operatorID: string, newProperty: Object): void {
    this.workflowModelEventService.operatorPropertyChangedSubject.next({operatorID, newProperty});
  }

}
