"""
Run database migration to add candidate info and scores to interviews table
"""
import pymysql
from pathlib import Path

def run_migration():
    """Run the migration SQL script"""
    
    print("=" * 60)
    print("Running Interview Database Migration")
    print("Adding candidate info and scoring fields")
    print("=" * 60)
    print()
    
    try:
        # Read the SQL file
        sql_file = Path(__file__).parent / "interview" / "migrations" / "add-candidate-info-and-scores.sql"
        
        with open(sql_file, 'r') as f:
            sql_content = f.read()
        
        # Connect to database
        connection = pymysql.connect(
            host='localhost',
            user='root',
            password='12345',
            database='recruit_db',
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        
        print("✅ Connected to database")
        
        with connection.cursor() as cursor:
            # Execute ALTER TABLE statements one by one
            alter_statements = [
                "ALTER TABLE interviews ADD COLUMN candidate_email VARCHAR(255) NULL AFTER candidate_name",
                "ALTER TABLE interviews ADD COLUMN job_title VARCHAR(255) NULL AFTER candidate_email",
                "ALTER TABLE interviews ADD COLUMN technical_score INT NULL COMMENT 'Technical knowledge score (0-100)' AFTER rating",
                "ALTER TABLE interviews ADD COLUMN communication_score INT NULL COMMENT 'Communication clarity score (0-100)' AFTER technical_score",
                "ALTER TABLE interviews ADD COLUMN problem_solving_score INT NULL COMMENT 'Problem solving score (0-100)' AFTER communication_score",
                "ALTER TABLE interviews ADD COLUMN cultural_fit_score INT NULL COMMENT 'Cultural fit score (0-100)' AFTER problem_solving_score",
                "ALTER TABLE interviews ADD COLUMN total_score INT NULL COMMENT 'Total score (0-100)' AFTER cultural_fit_score"
            ]
            
            for i, statement in enumerate(alter_statements, 1):
                try:
                    cursor.execute(statement)
                    connection.commit()
                    print(f"✅ Executed statement {i}")
                except pymysql.err.OperationalError as e:
                    # Ignore duplicate column errors
                    if 'Duplicate column name' in str(e):
                        print(f"⚠️  Column already exists (statement {i} skipped)")
                    else:
                        print(f"Error in statement {i}: {e}")
                        raise
        
        print()
        print("=" * 60)
        print("✅ Migration completed successfully!")
        print("=" * 60)
        print()
        print("Added fields to interviews table:")
        print("  ✓ candidate_name")
        print("  ✓ candidate_email")
        print("  ✓ job_title")
        print("  ✓ technical_score")
        print("  ✓ communication_score")
        print("  ✓ problem_solving_score")
        print("  ✓ cultural_fit_score")
        print("  ✓ total_score")
        print()
        
        connection.close()
        return True
        
    except Exception as e:
        print()
        print("=" * 60)
        print("❌ Migration failed!")
        print("=" * 60)
        print(f"Error: {e}")
        print()
        return False


if __name__ == "__main__":
    run_migration()
