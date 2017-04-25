import { Component, ViewChild } from '@angular/core';

import { MockDataService } from './services/mock-data-service';
import { CurrentDataService } from './services/current-data-service';

declare var jQuery: any;

@Component({
    moduleId: module.id,
    selector: 'my-app',
    template : `<div> Hello </div>`,
    providers: [MockDataService, CurrentDataService],
    styleUrls: ['style.css']
})
export class AppComponent {
	name = 'Angular';

    constructor(private currentDataService: CurrentDataService, private mockDataService: MockDataService) { }

}
