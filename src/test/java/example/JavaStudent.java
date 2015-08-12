package example;

/**
 * @author Kai Han
 */
public class JavaStudent {
	private long id;
	private String name;
	private int age;
	private short sex;
	private int rank;

	public JavaStudent(long id, String name, int age, short sex, int rank) {
		this.id = id;
		this.name = name;
		this.age = age;
		this.sex = sex;
		this.rank = rank;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public short getSex() {
		return sex;
	}

	public void setSex(short sex) {
		this.sex = sex;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	@Override
	public String toString() {
		return "JavaStudent [id=" + id + ", name=" + name + ", age=" + age + ", sex=" + sex + ", rank=" + rank + "]";
	}

}
