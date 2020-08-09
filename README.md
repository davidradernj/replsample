# Sample using PostgreSQL Logical Replication with JDBC

This is a very simple sample program for reading changes from a logical replication slot on an Amazon RDS PostgreSQL database using the PostgreSQL JDBC driver (pgjdbc). 

1. Use a custom parameter group on your RDS PostgreSQL instance. Set the rds.logical_replication parameter to "1". Reboot the instance.
2. Verify settings are correct by running "SHOW wal_level;" in psql (or any other PostgreSQL client tool).  Setting should be "logical". If not, try Step 1 again.
3. Test creating a logical replication slot to ensure settings are correct. Note, the sample program drops and recreates this same slot name using the test_decoding plugin.
```sql
SELECT * FROM pg_create_logical_replication_slot('sample_repl_slot', 'wal2json');
```
4. Create the user for reading the replication stream. The user account requires the rds_replication role to grant permissions to manage logical slots and to stream data using logical slots.
```sql
create user samp_repl_user with password '<your password>';
grant rds_replication to samp_repl_user;
```
5. Create a sample table to use for generating changes
```sql
create table foo (id int primary key, val text);
```
6. Modify the database URl and password in the sample program based on your instance and password you set in step 4. Then run the sample program. You should see output like the following.
```bash
Logical Replication Sample
Connected to the database
Dropping logical replication slot, just in case it was left.
Creating logical replication slot.
Starting logical replication stream.
Listening for replication messages
==================================
```
The program is now looping, waiting for for changes to stream. Leave it running.

7. Execute some commands to insert or change data in the table in your psql connection.
```sql
insert into foo values (1, 'hello');
insert into foo values (2, 'hello');
delete from foo where id=2;
insert into foo values (2, 'world');
```

In the console output of the sample program, you should see output like the following:
```bash
Listening for replication messages
==================================
BEGIN
table public.foo: INSERT: id[integer]:1 val[text]:'hello'
COMMIT
BEGIN
table public.foo: INSERT: id[integer]:2 val[text]:'hello'
COMMIT
BEGIN
table public.foo: DELETE: id[integer]:2
COMMIT
BEGIN
table public.foo: INSERT: id[integer]:2 val[text]:'world'
COMMIT
```

8. Clean up. When done, remember to drop the replication slot so that an inactive slot does not force the database server to keep WAL indefinitely:
```sql
select pg_drop_replication_slot('sample_repl_slot');
```

References:

PostgreSQL logical replication: https://www.postgresql.org/docs/current/logical-replication.html

pgjdbc support for logical (and physical) replication: https://jdbc.postgresql.org/documentation/head/replication.html

Using logical replication with RDS PostgreSQL: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html#PostgreSQL.Concepts.General.FeatureSupport.LogicalReplication

