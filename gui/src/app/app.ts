import { AfterViewInit, Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { retry, TimeoutError, timeout, timer } from 'rxjs';
import { User, UsersService } from './users.service';

@Component({
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSortModule,
    MatTableModule,
  ],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements AfterViewInit, OnInit {
  private readonly usersService = inject(UsersService);

  protected readonly displayedColumns = [
    'name',
    'username',
    'email',
    'address',
    'phone',
    'website',
    'company',
  ];
  protected readonly dataSource = new MatTableDataSource<User>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly errorMessage = signal('');

  @ViewChild(MatPaginator) private paginator!: MatPaginator;
  @ViewChild(MatSort) private sort!: MatSort;

  ngOnInit(): void {
    this.dataSource.filterPredicate = (user, filter) =>
      [
        user.name,
        user.username,
        user.email,
        user.address.street,
        user.address.suite,
        user.address.city,
        user.address.zipcode,
        user.phone,
        user.website,
        user.company.name,
      ]
        .join(' ')
        .toLowerCase()
        .includes(filter);
    this.loadUsers();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  protected loadUsers(): void {
    this.loading.set(true);
    this.error.set(false);
    this.errorMessage.set('');

    this.usersService
      .getUsers()
      .pipe(
        timeout({ first: 12_000 }),
        retry({
          count: 8,
          delay: (error: unknown, retryCount) => {
            if (
              !(error instanceof TimeoutError) &&
              (!(error instanceof HttpErrorResponse) ||
                ![0, 502, 503, 504].includes(error.status))
            ) {
              throw error;
            }
            return timer(Math.min(retryCount, 5) * 1000);
          },
        }),
      )
      .subscribe({
        next: (users) => {
          this.dataSource.data = users;
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(true);
          this.errorMessage.set(
            error instanceof TimeoutError
              ? 'The API did not respond in time. Check the API logs and try again.'
              : error.status === 502
                ? 'The API could not retrieve user data from JSONPlaceholder. Check the API logs and try again.'
                : error.status === 0 || error.status >= 502
                  ? 'The GUI could not connect to the API. Check that both services are running and try again.'
                  : `The API returned an unexpected error (HTTP ${error.status}).`,
          );
          this.loading.set(false);
        },
      });
  }

  protected applyFilter(event: Event): void {
    this.dataSource.filter = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.dataSource.paginator?.firstPage();
  }
}
