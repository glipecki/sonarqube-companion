import {Component} from '@angular/core';

@Component({
  selector: 'sq-navbar',
  template: `
    <div class="app-name">{{appName}}</div>
    <div class="menu">
      <a class="menu-item" routerLink="/overview" routerLinkActive="active">Overview</a>
      <a class="menu-item" routerLink="/project" routerLinkActive="active">Projects</a>
    </div>
    <div class="expander">
    </div>
    <div class="user-menu">
      <a class="menu-item settings-icon" routerLink="/settings" routerLinkActive="active">
        <i class="fa fa-cog"></i>
      </a>
    </div>
  `
})
export class NavbarComponent {
  appName = 'SQCompanion';
}
