import { TestBed, inject } from '@angular/core/testing';

import { ExecuteWorkflowService } from './execute-workflow.service';

import { WorkflowActionService } from './../workflow-graph/model/workflow-action.service';
import { OperatorMetadataService } from '../operator-metadata/operator-metadata.service';
import { StubOperatorMetadataService } from '../operator-metadata/stub-operator-metadata.service';
import { JointUIService } from '../joint-ui/joint-ui.service';
import { Observable } from 'rxjs/Observable';

import { mockExecutionResult } from './mock-result-data';
import { mockWorkflowPlan, mockLogicalPlan } from './mock-workflow-plan';
import { HttpClient } from '@angular/common/http';
import { marbles } from 'rxjs-marbles';
import { WorkflowGraph } from '../workflow-graph/model/workflow-graph';
import { LogicalPlan, SuccessExecutionResult } from '../../types/workflow-execute.interface';


class StubHttpClient {
  constructor() { }

  // fake an async http response with a very small delay
  public post<T>(url: string, body: string, headers: object): Observable<SuccessExecutionResult> {
    return Observable.of(mockExecutionResult);
  }

}

let service: ExecuteWorkflowService;

describe('ExecuteWorkflowService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ExecuteWorkflowService,
        WorkflowActionService,
        JointUIService,
        { provide: OperatorMetadataService, useClass: StubOperatorMetadataService },
        { provide: HttpClient, useClass: StubHttpClient}
      ]
    });

    service = TestBed.get(ExecuteWorkflowService);
  });

  it('should be created', inject([ExecuteWorkflowService], (injectedService: ExecuteWorkflowService) => {
    expect(injectedService).toBeTruthy();
  }));

  it('should generate a logical plan request based on the workflow graph that is passed to the function', marbles((m) => {
    const workflowGraph: WorkflowGraph = mockWorkflowPlan;
    const newLogicalPlan: LogicalPlan = ExecuteWorkflowService.getLogicalPlanRequest(workflowGraph);
    expect(newLogicalPlan).toEqual(mockLogicalPlan);
  }));

  it('should notify execution start event stream when an execution begins', marbles((m) => {
    const executionStartStream = service.getExecuteStartedStream()
      .map(value => 'a');

    m.hot('-a-').do(event => service.executeWorkflow()).subscribe();

    const expectedStream = m.hot('-a-');

    m.expect(executionStartStream).toBeObservable(expectedStream);

  }));

  it('should notify execution end event stream when a correct result is passed from backend', marbles((m) => {
    const executionEndStream = service.getExecuteEndedStream()
      .do(result => expect(result).toEqual(mockExecutionResult))
      .map(value => 'a');

    // execute workflow at this time
    m.hot('-a-').do(event => service.executeWorkflow()).subscribe();

    const expectedStream = m.hot('-a-');

    m.expect(executionEndStream).toBeObservable(expectedStream);

  }));

  it('should call post function when executing workflow', () => {
    const httpClient: HttpClient = TestBed.get(HttpClient);
    const postMethodSpy = spyOn(httpClient, 'post').and.callThrough();

    service.executeWorkflow();

    expect(postMethodSpy.calls.count()).toEqual(1);

  });

});
