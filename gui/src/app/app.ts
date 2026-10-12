import { AfterViewInit, Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
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

    this.usersService.getUsers().subscribe({
      next: (users) => {
        this.dataSource.data = users;
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  protected applyFilter(event: Event): void {
    this.dataSource.filter = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.dataSource.paginator?.firstPage();
  }
}
