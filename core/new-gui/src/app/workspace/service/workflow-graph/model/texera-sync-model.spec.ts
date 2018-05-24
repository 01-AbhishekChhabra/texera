import { TexeraSyncModel } from './texera-sync-model';
import { JointGraphReadonly } from './../../../types/joint-graph';
import { WorkflowGraph } from './../../../types/workflow-graph';
import { Point, OperatorPredicate, OperatorLink } from './../../../types/common.interface';
import {
  mockScanPredicate, mockResultPredicate, mockSentimentPredicate,
  mockScanResultLink, mockScanSentimentLink, mockSentimentResultLink, mockPoint
} from './mock-workflow-data';
import { Observable } from 'rxjs/Observable';
import { TestBed, inject } from '@angular/core/testing';
import { marbles, Context } from 'rxjs-marbles';

import '../../../../common/rxjs-operators';

import * as joint from 'jointjs';


describe('TexeraSyncModel', () => {

  let texeraGraph: WorkflowGraph;
  let jointGraphWrapper: JointGraphReadonly;

  function getAddOperatorValue(operator: OperatorPredicate) {
    return {
      operator: operator,
      point: mockPoint
    };
  }

  /**
   * Returns a mock JointJS operator Element object (joint.dia.Element)
   * The implementation code only uses the id attribute of the object.
   *
   * @param operatorID
   */
  function getJointOperatorValue(operatorID: string) {
    return {
      id: operatorID
    };
  }

  /**
   * Returns a mock JointJS Link object (joint.dia.Link)
   * It includes the attributes and functions same as JointJS
   *  and are used by the implementation code.
   * @param link
   */
  function getJointLinkValue(link: OperatorLink) {
    // getSourceElement, getTargetElement, and get all returns a function
    //  that returns the corresponding value
    return {
      id: link.linkID,
      getSourceElement: () => ({ id: link.source.operatorID }),
      getTargetElement: () => ({ id: link.target.operatorID }),
      get: (port: string) => {
        if (port === 'source') {
          return { port: link.source.portID };
        } else if (port === 'target') {
          return { port: link.target.portID };
        } else {
          throw new Error('getJointLinkValue: mock is inconsistent with implementation');
        }
      }
    };
  }

  /**
   * This helper function returns a mock JointJS link object (joint.dia.Link)
   *  that is only connected to a source port, but detached from the target port.
   *
   * This scenario happens when the user is still moving the link
   *  and it is not connected to a target port.
   *
   * @param link an operator link, but the target operator and target link is ignored
   */
  function getIncompleteJointLink(link: OperatorLink) {
    // getSourceElement, getTargetElement, and get all returns a function
    //  that returns the corresponding value
    return {
      id: link.linkID,
      getSourceElement: () => ({ id: link.source.operatorID }),
      getTargetElement: () => null,
      get: (port: string) => {
        if (port === 'source') {
          return { port: link.source.portID };
        } else if (port === 'target') {
          return null;
        } else {
          throw new Error('getJointLinkValue: mock is inconsistent with implementation');
        }
      }
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
      ]
    });

    texeraGraph = new WorkflowGraph();
    jointGraphWrapper = new JointGraphReadonly(new joint.dia.Graph());
  });

  /**
   * Test JointJS delete operator `onJointOperatorCellDelete` event stream handled properly
   *
   * Add one operator
   * Then emit one delete operator event from JointJS
   *
   * addOperator
   * jointDeleteOperator: ---d-|
   *
   * Expected:
   * The workflow graph should not have the added operator
   * The workflow graph should have 0 operators
   */
  it('should delete an operator when the delete operator event happens from JointJS', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);

    // prepare delete operator event stream
    const deleteOpMarbleString = '---d-|';
    const deleteOpMarbleValues = {
      d: getJointOperatorValue(mockScanPredicate.operatorID)
    };
    spyOn(jointGraphWrapper, 'onJointOperatorCellDelete').and.returnValue(
      m.hot(deleteOpMarbleString, deleteOpMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    // assert workflow graph
    jointGraphWrapper.onJointOperatorCellDelete().subscribe({
      complete: () => {
        expect(texeraGraph.hasOperator(mockScanPredicate.operatorID)).toBeFalsy();
        expect(texeraGraph.getOperators().length).toEqual(0);
      }
    });

  }));

  /**
   * Test JointJS delete operator `onJointOperatorCellDelete` event stream handled properly
   *
   * Add two operators
   * Then emit one delete operator event from JointJS
   *
   * addOperator
   * jointDeleteOperator: -----d-|
   *
   * Expected:
   * Only the deleted operator should be removed.
   * The graph should have 1 operators and 0 links.
   */
  it('should delete an operator and not touch other operators when the delete operator event happens from JointJS', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // prepare delete operator
    const deleteOpMarbleString = '-----d-|';
    const deleteOpMarbleValues = {
      d: getJointOperatorValue(mockScanPredicate.operatorID)
    };
    spyOn(jointGraphWrapper, 'onJointOperatorCellDelete').and.returnValue(
      m.hot(deleteOpMarbleString, deleteOpMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointOperatorCellDelete().subscribe({
      complete: () => {
        expect(texeraGraph.hasOperator(mockScanPredicate.operatorID)).toBeFalsy();
        expect(texeraGraph.hasOperator(mockResultPredicate.operatorID)).toBeTruthy();
        expect(texeraGraph.getOperators().length).toEqual(1);
        expect(texeraGraph.getLinks().length).toEqual(0);
      }
    });

  }));


  /**
   * Test JointJS delete operator `onJointOperatorCellDelete` event stream handled properly
   *
   * Add two operators
   * Delete on operator (caused by deleteOperator calling from the code)
   * Operator will also be deleted in JointJS, jointOperatorCellDelete will be emitted,
   *  causing the handler trying to delete the same operator *twice*
   * The second delete should be ignored because the operator is already deleted.
   *
   * addOperator
   * jointDeleteOperator: -----d-|
   *
   * Expected:
   * one operator is deleted
   * no error is thrown
   */
  it('should ignore duplicate delete operator event from JointJS if the operator is deleted from the code', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    texeraGraph.deleteOperator(mockScanPredicate.operatorID);

    // prepare delete operator
    const deleteOpMarbleString = '-----d-|';
    const deleteOpMarbleValues = {
      d: getJointOperatorValue(mockScanPredicate.operatorID)
    };
    // mock delete the operator operation at the same time frame of jointJS deleting it
    //  but executed before the handler
    spyOn(jointGraphWrapper, 'onJointOperatorCellDelete').and.returnValue(
      m.hot(deleteOpMarbleString, deleteOpMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    // no error should be thrown
    jointGraphWrapper.onJointOperatorCellDelete().subscribe({
      complete: () => {
        expect(texeraGraph.hasOperator(mockScanPredicate.operatorID)).toBeFalsy();
        expect(texeraGraph.hasOperator(mockResultPredicate.operatorID)).toBeTruthy();
        expect(texeraGraph.getOperators().length).toEqual(1);
        expect(texeraGraph.getLinks().length).toEqual(0);
      }
    });

  }));

  /**
   * Test JointJS add link `onJointLinkCellAdd` event stream handled properly
   *
   * Add two operators
   * Then emit one add link event from JointJS
   *
   * addOperator
   * jointAddLink:  -----p-|
   *
   * Expected:
   * The graph should have two operators and a link between the operators
   */
  it('should add a link when link add event happen from JointJS', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // prepare add link
    const addLinkMarbleString = '-----p-|';
    const addLinkMarbleValues = {
      p: getJointLinkValue(mockScanResultLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellAdd').and.returnValue(
      m.hot(addLinkMarbleString, addLinkMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointLinkCellAdd().subscribe({
      complete: () => {
        expect(texeraGraph.getOperators().length).toEqual(2);
        expect(texeraGraph.getLinks().length).toEqual(1);
        expect(texeraGraph.hasLinkWithID(mockScanResultLink.linkID)).toBeTruthy();
        expect(texeraGraph.getLinkWithID(mockScanResultLink.linkID)).toEqual(mockScanResultLink);
        expect(texeraGraph.hasLink(
          mockScanResultLink.source, mockScanResultLink.target
        )).toBeTruthy();
      }
    });

  }));

  /**
   * Test JointJS add link `onJointLinkCellAdd` event stream handled properly
   *  when the added JointJS link is invalid.
   *
   * Add two operators
   * Then a user drags a link from a source port,
   *  the link is visually added,
   *  but the link is not yet connected to a target port.
   * This link is considered invalid and should not appear in the graph
   *
   * addOperator
   * jointAddLink:  -----q-| (q is an incomplete Joint link)
   *
   * Expected:
   * The graph doesn't contain the incomplete link
   */
  it('should not create a link when an incomplete link is added in JointJS', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // prepare add link (incomplete link)
    const addLinkMarbleString = '-----q-|';
    const addLinkMarbleValues = {
      q: getIncompleteJointLink(mockScanResultLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellAdd').and.returnValue(
      m.hot(addLinkMarbleString, addLinkMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointLinkCellDelete().subscribe({
      complete: () => {
        expect(texeraGraph.getLinks().length).toEqual(0);
      }
    });

  }));

  /**
   * Test JointJS delete link `onJointLinkCellDelete` event stream handled properly
   *
   * Add two operators and one link
   * Then emit one delete link event from JointJS
   *
   * add operators + links: 1 -> 2
   * jointDeleteLink: -------r-|
   *
   * Expected:
   * The link should be deleted
   */
  it('should delete a link when link delete event happen from JointJS', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // add links
    texeraGraph.addLink(mockScanResultLink);

    // prepare delete link
    const deleteLinkMarbleString = '-------r-|';
    const deleteLinkMarbleValues = {
      r: getJointLinkValue(mockScanResultLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellDelete').and.returnValue(
      m.hot(deleteLinkMarbleString, deleteLinkMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointLinkCellDelete().subscribe({
      complete: () => {
        expect(texeraGraph.getLinks().length).toEqual(0);
      }
    });

  }));

  /**
   * Test JointJS delete link `onJointLinkCellDelete` event stream handled properly,
   *  when the deleted link is invalid and never existed in texera graph.
   *
   * Add two operators
   * Then a user drags a link from a source port,
   *  the link is visually added,
   *  but the link is not yet connected to a target port.
   * Then the user release the mouse and the link is visually deleted,
   *  JointJS emits Link Delete event,
   *  the workflow graph should ignore it.
   *
   * add operators
   * jointAddLink:    -----q-| (q is an incomplete Joint link)
   * jointDeleteLink: -------r-| (the visual deletion of the incomplete link)
   *
   * Expected:
   * The graph doesn't contain the link
   */
  it('should ignore JointJS link delete event of an incomplete link', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // prepare add link (incomplete link)
    const addLinkMarbleString = '-----q-|';
    const addLinkMarbleValues = {
      q: getIncompleteJointLink(mockScanResultLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellAdd').and.returnValue(
      m.hot(addLinkMarbleString, addLinkMarbleValues)
    );

    // prepare delete link (incomplete link)
    const deleteLinkMarbleString = '-------r-|';
    const deleteLinkMarbleValues = {
      r: getIncompleteJointLink(mockScanResultLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellDelete').and.returnValue(
      m.hot(deleteLinkMarbleString, deleteLinkMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointLinkCellAdd().subscribe({
      complete: () => {
        expect(texeraGraph.getLinks().length).toEqual(0);
      }
    });

  }));

  /**
   * Test JointJS link change `onJointLinkCellChange` event stream handled properly,
   *  when the link change involves logical link delete
   *
   * Add two operators
   * Then add a link of these operators
   * Then the user drags the target port of the connected link,
   *   the link is detached from the target port.
   * This link is now considered invalid and should be deleted from the graph
   *
   * add operators and links: 1 -> 2
   * changeLink:  -------q-| (link changes: detached from the target)
   *
   * The detatched link should be deleted from the graph.
   */
  it('should delete the link when a link is detached from the target port', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // add links
    texeraGraph.addLink(mockScanResultLink);

    // prepare change link (link detached from target port)
    const changeLinkMarbleString = '-------q-|';
    const changeLinkMarbleValues = {
      q: getIncompleteJointLink(mockScanResultLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellChange').and.returnValue(
      m.hot(changeLinkMarbleString, changeLinkMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointLinkCellChange().subscribe({
      complete: () => {
        expect(texeraGraph.getLinks().length).toEqual(0);
      }
    });

  }));

  /**
   * Test JointJS link change `onJointLinkCellChange` event stream handled properly,
   *  when the link change involves logical link delete,
   *  and the same change event involves an *immediate* link add.
   *
   * Add three operators
   * Then add a link from operator 1 to operator 2
   * Then the user directly drags the target port from operator 2's input operator
   *  to operator 3's input port. The link automatically attach to operator3's target port,
   *  and JointJS only emits one link change event,
   *
   * addOperators: 1 -> 2 (will change to 1 -> 3 in after changeLink event)
   * addLink:     -------p-|
   * changeLink:  ---------t-| (link changes: target operator/port changed)
   *
   * Expected:
   * the link should be changed to the new target
   *
   */
  it('should delete and then re-add the link if link target is changed from one port to another', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockSentimentPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // prepare add link
    const addLinkMarbleString = '-------p-|';
    const addLinkMarbleValues = {
      p: getJointLinkValue(mockScanResultLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellAdd').and.returnValue(
      m.hot(addLinkMarbleString, addLinkMarbleValues)
    );

    // create a mock changed link using another link's source/target
    // but the link ID remains the same
    const mockChangedLink = {
      ...mockScanSentimentLink,
      linkID: mockScanResultLink.linkID
    };

    // prepare change link (link detached from target port)
    const changeLinkMarbleString = '---------t-|';
    const changeLinkMarbleValues = {
      t: getJointLinkValue(mockChangedLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellChange').and.returnValue(
      m.hot(changeLinkMarbleString, changeLinkMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointLinkCellChange()
      .subscribe({
        complete: () => {
          expect(texeraGraph.getLinks().length).toEqual(1);
          expect(texeraGraph.hasLinkWithID(mockChangedLink.linkID)).toBeTruthy();
          expect(texeraGraph.getLinkWithID(mockChangedLink.linkID)).toEqual(mockChangedLink);
          expect(texeraGraph.hasLink(
            mockScanResultLink.source, mockScanResultLink.target
          )).toBeFalsy();
          expect(texeraGraph.hasLink(
            mockChangedLink.source, mockChangedLink.target
          )).toBeTruthy();
        }
      });

  }));

  /**
   * Test JointJS link change `onJointLinkCellChange` event stream handled properly,
   *  when the link change involves logical link delete,
   *  and a later change event involves a logical link add.
   *
   * Add three operators (1, 2, 3) and link 1 -> 3
   * Then the user *gradually* drags the target port from operator 3's input port
   *  to operator 2's input port. (1 -> 3) changed to (1 -> 2)
   * The link is detached, then move around the paper for a while, then re-attached to another port
   *
   * changeLink:  ---------q-r-s-t-| (q: link detached with target being a point, r: target moved to another point,
   *    s: target moved to another point, t: target re-attached to another port)
   *
   * Expected:
   * the link should be changed to the new target.
   *
   * TODO: finish change link test stream to compare to streams
   */
  it('should remove then add link if link target port is detached then dragged around then re-attached', marbles((m) => {

    // add operators
    texeraGraph.addOperator(mockScanPredicate);
    texeraGraph.addOperator(mockSentimentPredicate);
    texeraGraph.addOperator(mockResultPredicate);

    // add links
    texeraGraph.addLink(mockScanResultLink);

    // create a mock changed link using another link's source/target
    // but the link ID remains the same
    const mockChangedLink = {
      ...mockScanSentimentLink,
      linkID: mockScanResultLink.linkID,
    };

    // prepare change link (link detached from target port)
    const changeLinkMarbleString = '---------q-r-s-t-|';
    const changeLinkMarbleValues = {
      q: getIncompleteJointLink(mockScanResultLink),
      r: getIncompleteJointLink(mockScanResultLink),
      s: getIncompleteJointLink(mockScanResultLink),
      t: getJointLinkValue(mockChangedLink)
    };
    spyOn(jointGraphWrapper, 'onJointLinkCellChange').and.returnValue(
      m.hot(changeLinkMarbleString, changeLinkMarbleValues)
    );

    // construct the texera sync model with spied dependencies
    const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);

    jointGraphWrapper.onJointLinkCellChange().subscribe({
      complete: () => {
        expect(texeraGraph.getLinks().length).toEqual(1);
        expect(texeraGraph.hasLink(
          mockChangedLink.source, mockChangedLink.target
        )).toBeTruthy();
      }
    });

    // assert link delete stream: delete original link
    const linkDeleteStream = texeraGraph.getLinkDeleteStream();
    const expectedDeleteStream = m.hot('---------q---', { q: { deletedLink: mockScanResultLink } });
    m.expect(linkDeleteStream).toBeObservable(expectedDeleteStream);

    // assert link add stream: changed link after its re-attached (original link is added synchronously in the begining)
    const linkAddStream = texeraGraph.getLinkAddStream();
    const expectedAddStream = m.hot('---------------t-', { t: mockChangedLink });
    m.expect(linkAddStream).toBeObservable(expectedAddStream);

  }));

  /**
   * Test JointJS delete operator `onJointOperatorCellDelete` event stream handled properly,
   *  when the operator delete causes its connected links being deleted as well
   *
   * Add three operators
   * Then add a link from operator 1 to operator 2 and a link from operator 2 to operator 3
   *
   * addOperators + addLinks: 1 -> 2 -> 3
   * jointDeleteOperator:  ---------d-| (delete operator 2)
   * jointDeleteLink:      ---------(gh)-| (mock event triggered automatically at the same time frame by jointJS)
   *
   * Expected:
   * There will be 2 operators left
   * There will be no links left
   * Texera Operator Delete stream should emit event when the operator is deleted
   * Texera Link Delete Stream should emit event twice when the operator is deleted
   *
   */
  it('should remove an operator and its connected links when that operator is deleted from jointJS',
    marbles((m) => {
      // add operators
      texeraGraph.addOperator(mockScanPredicate);
      texeraGraph.addOperator(mockSentimentPredicate);
      texeraGraph.addOperator(mockResultPredicate);

      // add links
      texeraGraph.addLink(mockScanSentimentLink);
      texeraGraph.addLink(mockSentimentResultLink);

      // prepare the delete oprator event
      const deleteOperatorString = '---------d-|';
      const deleteOperatorValue = {
        d: getJointOperatorValue(mockSentimentPredicate.operatorID)
      };
      spyOn(jointGraphWrapper, 'onJointOperatorCellDelete').and.returnValue(
        m.hot(deleteOperatorString, deleteOperatorValue)
      );

      /**
       * once the operator is deleted, JointJS will automatically delete connected links
       *  and will trigger delete link events at the same timeframe
       */
      const deleteLinkString = '---------(gh)-|';
      const deleteLinkValue = {
        g: getJointLinkValue(mockScanSentimentLink),
        h: getJointLinkValue(mockSentimentResultLink)
      };

      spyOn(jointGraphWrapper, 'onJointLinkCellDelete').and.returnValue(
        m.hot(deleteLinkString, deleteLinkValue)
      );

      // construct texera model
      const texeraSyncModel = new TexeraSyncModel(texeraGraph, jointGraphWrapper);
      jointGraphWrapper.onJointOperatorCellDelete().subscribe({
        complete: () => {
          expect(texeraGraph.hasOperator(mockSentimentPredicate.operatorID)).toBeFalsy();
          expect(texeraGraph.hasOperator(mockScanPredicate.operatorID)).toBeTruthy();
          expect(texeraGraph.hasOperator(mockResultPredicate.operatorID)).toBeTruthy();
          expect(texeraGraph.getOperators().length).toEqual(2);
          expect(texeraGraph.getLinkWithID(mockScanSentimentLink.linkID)).toBe(undefined);
          expect(texeraGraph.getLinkWithID(mockSentimentResultLink.linkID)).toBe(undefined);
          expect(texeraGraph.getLinks().length).toEqual(0);
        }
      });

    }));


});
