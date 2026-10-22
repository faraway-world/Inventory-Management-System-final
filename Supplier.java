public class Supplier {
	private String id;
	private String name;
	private String phone;
	private String email;

	public Supplier(String id, String name, String phone, String email) {
		if (id == null || id.trim().isEmpty()) {
			throw new IllegalArgumentException("Supplier ID cannot be empty");
		}
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Supplier name cannot be empty");
		}
		this.id = id;
		this.name = name;
		this.phone = phone;
		this.email = email;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public void setName(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Supplier name cannot be empty");
		}
		this.name = name;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String toString() {
		return id + " | " + name + " | " + phone + " | " + email;
	}
}